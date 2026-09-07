package com.example.application.attendance_module.service;

import com.example.application.attendance_module.dto.CorrectionRequestCreateRequest;
import com.example.application.attendance_module.dto.CorrectionRequestResponse;
import com.example.application.attendance_module.dto.CorrectionReviewRequest;
import com.example.application.attendance_module.entity.Attendance;
import com.example.application.attendance_module.entity.AttendanceCorrectionRequest;
import com.example.application.attendance_module.entity.AttendanceRuleConfig;
import com.example.application.attendance_module.repository.AttendanceCorrectionRequestRepository;
import com.example.application.attendance_module.repository.AttendanceRepository;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Employee-facing "request a fix" workflow, and the admin approve/reject side of it. Approving a
 * request never silently overwrites the attendance record - it goes through the exact same
 * AttendanceRulesEngine.compute() that a normal check-out uses, so an approved correction is
 * computed exactly the same way a normal day would have been. Every request/approve/reject is
 * logged through the same shared AuditService every other module uses.
 */
@Service
public class AttendanceCorrectionService {

    private static final List<String> VALID_REQUEST_TYPES = List.of(
            "MISSING_CHECK_IN", "MISSING_CHECK_OUT", "WRONG_CHECK_IN", "WRONG_CHECK_OUT", "OTHER");

    private final AttendanceCorrectionRequestRepository correctionRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSiteAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;
    private final AttendanceRuleConfigService ruleConfigService;
    private final AttendanceRulesEngine ruleEngine;

    public AttendanceCorrectionService(AttendanceCorrectionRequestRepository correctionRepository,
                                        AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository,
                                        EmployeeSiteAssignmentRepository assignmentRepository, UserRepository userRepository,
                                        TenantContextService tenantContext,
                                        AuditService auditService, AttendanceRuleConfigService ruleConfigService,
                                        AttendanceRulesEngine ruleEngine) {
        this.correctionRepository = correctionRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
        this.ruleConfigService = ruleConfigService;
        this.ruleEngine = ruleEngine;
    }

    /** Always resolves the employee from the CALLER's own login - same structural guarantee as
        markMine()/checkIn()/checkOut(): there is no way to request a correction for anyone but yourself. */
    @Transactional
    public CorrectionRequestResponse create(Long userId, CorrectionRequestCreateRequest request, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        if (!VALID_REQUEST_TYPES.contains(request.getRequestType())) {
            throw new BadRequestException("Request type must be one of: " + String.join(", ", VALID_REQUEST_TYPES));
        }
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));

        AttendanceCorrectionRequest correction = new AttendanceCorrectionRequest();
        correction.setClientCompanyId(tenantId);
        correction.setEmployeeId(employee.getId());
        correction.setAttendanceDate(request.getAttendanceDate());
        correction.setRequestType(request.getRequestType());
        correction.setRequestedCheckIn(request.getRequestedCheckIn());
        correction.setRequestedCheckOut(request.getRequestedCheckOut());
        correction.setReason(request.getReason());
        correction.setCreatedBy(employee.getUser() != null ? employee.getUser().getId() : userId);

        AttendanceCorrectionRequest saved = correctionRepository.save(correction);
        auditService.log(userId, "CORRECTION_REQUESTED",
                employee.getEmployeeCode() + " requested a " + request.getRequestType()
                        + " correction for " + request.getAttendanceDate(), httpRequest);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CorrectionRequestResponse> findMine(Long userId, Pageable pageable) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No employee profile is linked to this login."));
        return correctionRepository.findAllByClientCompanyIdAndEmployeeIdOrderByCreatedAtDesc(tenantId, employee.getId(), pageable)
                .map(this::toResponse);
    }

    /** Admin/HR view - all requests, optionally filtered to one status (e.g. "PENDING" for a review queue). */
    @Transactional(readOnly = true)
    public Page<CorrectionRequestResponse> findAll(String status, Pageable pageable) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Page<AttendanceCorrectionRequest> page = (status != null && !status.isBlank())
                ? correctionRepository.findAllByClientCompanyIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable)
                : correctionRepository.findAllByClientCompanyIdOrderByCreatedAtDesc(tenantId, pageable);
        return page.map(this::toResponse);
    }

    /**
     * Approving updates the ORIGINAL centralized attendance record for that employee/date
     * (creating it first if the day was never marked at all, e.g. a genuinely missed check-in)
     * - never a separate "corrected" record. Re-runs AttendanceRulesEngine.compute() whenever
     * both a check-in and check-out end up present, so late/early/hours/status are always
     * derived the same way check-out itself derives them, never hand-set.
     */
    @Transactional
    public CorrectionRequestResponse approve(Long id, CorrectionReviewRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        AttendanceCorrectionRequest correction = correctionRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Correction request does not belong to the current tenant"));
        if (!AttendanceCorrectionRequest.STATUS_PENDING.equals(correction.getStatus())) {
            throw new BadRequestException("This request has already been " + correction.getStatus().toLowerCase() + ".");
        }

        Attendance attendance = attendanceRepository
                .findByClientCompanyIdAndEmployeeIdAndAttendanceDate(tenantId, correction.getEmployeeId(), correction.getAttendanceDate())
                .orElseGet(() -> {
                    Attendance fresh = new Attendance();
                    fresh.setClientCompanyId(tenantId);
                    fresh.setEmployeeId(correction.getEmployeeId());
                    fresh.setAttendanceDate(correction.getAttendanceDate());
                    Long siteId = assignmentRepository
                            .findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(correction.getEmployeeId(), tenantId, "ACTIVE")
                            .map(a -> a.getSiteId())
                            .orElseThrow(() -> new BadRequestException(
                                    "This employee has no active site assignment - cannot create an attendance record for them."));
                    fresh.setSiteId(siteId);
                    return fresh;
                });

        if (correction.getRequestedCheckIn() != null) {
            attendance.setCheckInTime(correction.getRequestedCheckIn());
        }
        if (correction.getRequestedCheckOut() != null) {
            attendance.setCheckOutTime(correction.getRequestedCheckOut());
        }
        attendance.setAttendanceSource("ADMIN");
        attendance.setUpdatedBy(actorId);
        if (attendance.getMarkedBy() == null) {
            attendance.setMarkedBy(actorId);
        }

        if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
            AttendanceRuleConfig config = ruleConfigService.getOrCreateForCurrentTenant();
            AttendanceRulesEngine.WorkResult result = ruleEngine.compute(attendance.getCheckInTime(), attendance.getCheckOutTime(), config);
            attendance.setGrossWorkMinutes(result.grossWorkMinutes);
            attendance.setBreakMinutes(result.breakMinutes);
            attendance.setNetWorkMinutes(result.netWorkMinutes);
            attendance.setLate(result.late);
            attendance.setLateMinutes(result.lateMinutes);
            attendance.setEarlyExit(result.earlyExit);
            attendance.setEarlyExitMinutes(result.earlyExitMinutes);
            attendance.setStatus(result.status);
        }
        attendanceRepository.save(attendance);

        correction.setStatus(AttendanceCorrectionRequest.STATUS_APPROVED);
        correction.setReviewedBy(actorId);
        correction.setReviewedAt(LocalDateTime.now());
        correction.setReviewRemarks(request.getRemarks());
        AttendanceCorrectionRequest saved = correctionRepository.save(correction);

        auditService.log(actorId, "CORRECTION_APPROVED",
                "Approved correction request " + id + " for employee id " + correction.getEmployeeId()
                        + " on " + correction.getAttendanceDate(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public CorrectionRequestResponse reject(Long id, CorrectionReviewRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        AttendanceCorrectionRequest correction = correctionRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Correction request does not belong to the current tenant"));
        if (!AttendanceCorrectionRequest.STATUS_PENDING.equals(correction.getStatus())) {
            throw new BadRequestException("This request has already been " + correction.getStatus().toLowerCase() + ".");
        }

        correction.setStatus(AttendanceCorrectionRequest.STATUS_REJECTED);
        correction.setReviewedBy(actorId);
        correction.setReviewedAt(LocalDateTime.now());
        correction.setReviewRemarks(request.getRemarks());
        AttendanceCorrectionRequest saved = correctionRepository.save(correction);

        auditService.log(actorId, "CORRECTION_REJECTED",
                "Rejected correction request " + id + " for employee id " + correction.getEmployeeId()
                        + " on " + correction.getAttendanceDate(), httpRequest);
        return toResponse(saved);
    }

    private CorrectionRequestResponse toResponse(AttendanceCorrectionRequest c) {
        CorrectionRequestResponse r = new CorrectionRequestResponse();
        r.setId(c.getId());
        r.setEmployeeId(c.getEmployeeId());
        employeeRepository.findById(c.getEmployeeId()).ifPresent(e -> {
            r.setEmployeeCode(e.getEmployeeCode());
            r.setEmployeeName(e.getFirstName() + " " + e.getLastName());
        });
        r.setAttendanceDate(c.getAttendanceDate());
        r.setRequestType(c.getRequestType());
        r.setRequestedCheckIn(c.getRequestedCheckIn());
        r.setRequestedCheckOut(c.getRequestedCheckOut());
        r.setReason(c.getReason());
        r.setStatus(c.getStatus());
        if (c.getReviewedBy() != null) {
            userRepository.findById(c.getReviewedBy()).ifPresent(u -> r.setReviewedByUsername(u.getUsername()));
        }
        r.setReviewedAt(c.getReviewedAt());
        r.setReviewRemarks(c.getReviewRemarks());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }
}
