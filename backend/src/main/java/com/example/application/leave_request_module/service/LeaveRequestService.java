package com.example.application.leave_request_module.service;

import com.example.application.attendance_module.service.AttendanceService;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.leave_request_module.dto.LeaveRequestAdminCreateRequest;
import com.example.application.leave_request_module.dto.LeaveRequestCreateRequest;
import com.example.application.leave_request_module.dto.LeaveRequestResponse;
import com.example.application.leave_request_module.dto.LeaveRequestReviewRequest;
import com.example.application.leave_request_module.entity.LeaveRequest;
import com.example.application.leave_request_module.repository.LeaveRequestRepository;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Leave Request Workflow.
 *
 * Two ways in:
 *   - selfCreate() - an employee with login (and LEAVE_REQUEST_SELF_CREATE) applies for their
 *     own leave. Starts PENDING; needs approve()/reject() from an admin/supervisor.
 *   - adminCreate() - an admin/supervisor adds leave directly for ANY employee (typically one
 *     without login, who can't self-request at all) - goes straight to APPROVED, since the
 *     person creating it already IS the approver. No separate review step.
 *
 * Either way, the moment a request becomes APPROVED, every date in its range gets marked
 * ON_LEAVE in Attendance (AttendanceService.markOnLeaveForRange()) - Payroll already treats
 * ON_LEAVE rows as paid/unpaid leave automatically based on the employee's Paid Leave balance
 * (see PayrollInputResolver), so this is the ONLY integration point needed for leave to flow
 * into salary calculation correctly - no separate payroll-side leave logic required.
 */
@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AttendanceService attendanceService;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, EmployeeRepository employeeRepository,
                                UserRepository userRepository, AttendanceService attendanceService,
                                TenantContextService tenantContext, AuditService auditService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.attendanceService = attendanceService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> findAll() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return leaveRequestRepository.findAllByClientCompanyIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse).toList();
    }

    /** The logged-in employee's own leave requests - resolved from their User account, same pattern as PayslipService.generateMyPayslip(). */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> findMine(Long userId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));
        return leaveRequestRepository.findAllByClientCompanyIdAndEmployeeIdOrderByCreatedAtDesc(tenantId, employee.getId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public LeaveRequestResponse selfCreate(LeaveRequestCreateRequest request, Long userId, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date.");
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setClientCompanyId(tenantId);
        leaveRequest.setEmployeeId(employee.getId());
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus("PENDING");
        leaveRequest.setSelfRequested(true);
        leaveRequest.setCreatedBy(actorId);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        auditService.log(actorId, "LEAVE_REQUEST_CREATED",
                employee.getEmployeeCode() + " applied for leave " + saved.getStartDate() + " to " + saved.getEndDate(), httpRequest);
        return toResponse(saved);
    }

    /** For an employee without login (or any employee) - an admin/supervisor adds leave directly, already approved, since the person adding it IS the approver. */
    @Transactional
    public LeaveRequestResponse adminCreate(LeaveRequestAdminCreateRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = employeeRepository.findByIdAndClientCompanyId(request.getEmployeeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date.");
        }

        attendanceService.markOnLeaveForRange(tenantId, employee.getId(), request.getStartDate(), request.getEndDate(),
                "Leave - " + (request.getReason() != null ? request.getReason() : "added by admin"), actorId);

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setClientCompanyId(tenantId);
        leaveRequest.setEmployeeId(employee.getId());
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus("APPROVED");
        leaveRequest.setSelfRequested(false);
        leaveRequest.setReviewedBy(actorId);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setCreatedBy(actorId);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        auditService.log(actorId, "LEAVE_REQUEST_ADDED",
                "Leave added directly for " + employee.getEmployeeCode() + " (" + saved.getStartDate() + " to " + saved.getEndDate() + ")", httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse approve(Long id, LeaveRequestReviewRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        LeaveRequest leaveRequest = getPendingRequest(id, tenantId);

        attendanceService.markOnLeaveForRange(tenantId, leaveRequest.getEmployeeId(), leaveRequest.getStartDate(), leaveRequest.getEndDate(),
                "Leave - " + (leaveRequest.getReason() != null ? leaveRequest.getReason() : "approved"), actorId);

        leaveRequest.setStatus("APPROVED");
        leaveRequest.setReviewedBy(actorId);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setReviewNote(request != null ? request.getReviewNote() : null);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        auditService.log(actorId, "LEAVE_REQUEST_APPROVED",
                "Approved leave request #" + saved.getId() + " (" + saved.getStartDate() + " to " + saved.getEndDate() + ")", httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse reject(Long id, LeaveRequestReviewRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        LeaveRequest leaveRequest = getPendingRequest(id, tenantId);

        leaveRequest.setStatus("REJECTED");
        leaveRequest.setReviewedBy(actorId);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setReviewNote(request != null ? request.getReviewNote() : null);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        auditService.log(actorId, "LEAVE_REQUEST_REJECTED",
                "Rejected leave request #" + saved.getId(), httpRequest);
        return toResponse(saved);
    }

    private LeaveRequest getPendingRequest(Long id, Long tenantId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + id));
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            throw new BadRequestException("This leave request has already been " + leaveRequest.getStatus().toLowerCase() + ".");
        }
        return leaveRequest;
    }

    private LeaveRequestResponse toResponse(LeaveRequest lr) {
        LeaveRequestResponse response = new LeaveRequestResponse();
        response.setId(lr.getId());
        response.setEmployeeId(lr.getEmployeeId());
        employeeRepository.findById(lr.getEmployeeId()).ifPresent(e -> {
            response.setEmployeeCode(e.getEmployeeCode());
            response.setEmployeeName(e.getFirstName() + " " + e.getLastName());
        });
        response.setStartDate(lr.getStartDate());
        response.setEndDate(lr.getEndDate());
        response.setDayCount(ChronoUnit.DAYS.between(lr.getStartDate(), lr.getEndDate()) + 1);
        response.setReason(lr.getReason());
        response.setStatus(lr.getStatus());
        response.setSelfRequested(lr.isSelfRequested());
        if (lr.getReviewedBy() != null) {
            User reviewer = userRepository.findById(lr.getReviewedBy()).orElse(null);
            response.setReviewedByName(reviewer != null ? reviewer.getFirstName() + " " + reviewer.getLastName() : null);
        }
        response.setReviewedAt(lr.getReviewedAt());
        response.setReviewNote(lr.getReviewNote());
        response.setCreatedAt(lr.getCreatedAt());
        return response;
    }
}
