package com.example.application.exit_module.service;

import com.example.application.advance_module.service.EmployeeAdvanceService;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.employee_module.service.EmployeeService;
import com.example.application.exit_module.dto.EmployeeExitRequest;
import com.example.application.exit_module.dto.EmployeeExitResponse;
import com.example.application.exit_module.entity.EmployeeExit;
import com.example.application.exit_module.repository.EmployeeExitRepository;
import com.example.application.salary_structure_module.entity.EmployeeSalaryStructure;
import com.example.application.salary_structure_module.repository.EmployeeSalaryStructureRepository;
import com.example.application.salary_structure_module.service.SalaryStructureService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Exit Management (Full & Final Settlement).
 *
 * Two-step lifecycle:
 *   1. initiate() - resignation is recorded (status INITIATED). The employee stays ACTIVE and
 *      keeps working/getting attendance marked normally through their notice period - nothing
 *      about payroll or attendance changes yet.
 *   2. settle() - run on/after the last working day. Computes the prorated salary for the exit
 *      month + deducts any outstanding advance balance, persists those figures permanently
 *      (status SETTLED), and deactivates the Employee (EmployeeService.deactivate()) so they
 *      stop appearing in future attendance/payroll cycles.
 *
 * Prorated salary is a deliberately simple calculation: (this month's Gross Earnings from their
 * salary structure effective on the last working day) x (day-of-month of the last working day) /
 * (total days in that month) - i.e. it assumes they worked every day of the exit month up to
 * their last working day. It does NOT cross-check actual attendance for that month; a more
 * precise version could, but this keeps the settlement calculation transparent and predictable
 * for a first version of this feature.
 */
@Service
public class ExitService {

    private final EmployeeExitRepository exitRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSalaryStructureRepository employeeSalaryStructureRepository;
    private final SalaryStructureService salaryStructureService;
    private final EmployeeAdvanceService employeeAdvanceService;
    private final EmployeeService employeeService;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public ExitService(EmployeeExitRepository exitRepository, EmployeeRepository employeeRepository,
                        EmployeeSalaryStructureRepository employeeSalaryStructureRepository,
                        SalaryStructureService salaryStructureService, EmployeeAdvanceService employeeAdvanceService,
                        EmployeeService employeeService, TenantContextService tenantContext, AuditService auditService) {
        this.exitRepository = exitRepository;
        this.employeeRepository = employeeRepository;
        this.employeeSalaryStructureRepository = employeeSalaryStructureRepository;
        this.salaryStructureService = salaryStructureService;
        this.employeeAdvanceService = employeeAdvanceService;
        this.employeeService = employeeService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EmployeeExitResponse> findAll() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return exitRepository.findAllByClientCompanyIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeExitResponse initiate(EmployeeExitRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();

        Employee employee = employeeRepository.findByIdAndClientCompanyId(request.getEmployeeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new BadRequestException("This employee is already inactive.");
        }
        if (request.getLastWorkingDay().isBefore(request.getResignationDate())) {
            throw new BadRequestException("Last working day cannot be before the resignation date.");
        }
        exitRepository.findFirstByClientCompanyIdAndEmployeeIdAndStatus(tenantId, employee.getId(), "INITIATED")
                .ifPresent(existing -> {
                    throw new BadRequestException("This employee already has a resignation in progress (recorded " + existing.getResignationDate() + ").");
                });

        EmployeeExit exit = new EmployeeExit();
        exit.setClientCompanyId(tenantId);
        exit.setEmployeeId(employee.getId());
        exit.setResignationDate(request.getResignationDate());
        exit.setLastWorkingDay(request.getLastWorkingDay());
        exit.setReason(request.getReason());
        exit.setStatus("INITIATED");
        exit.setCreatedBy(actorId);
        EmployeeExit saved = exitRepository.save(exit);

        auditService.log(actorId, "EMPLOYEE_EXIT_INITIATED",
                "Recorded resignation for " + employee.getEmployeeCode() + " - last working day " + saved.getLastWorkingDay(), httpRequest);

        return toResponse(saved);
    }

    /** Read-only preview of the settlement figures, without persisting anything - lets an admin review before committing via settle(). */
    @Transactional(readOnly = true)
    public EmployeeExitResponse previewSettlement(Long exitId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        EmployeeExit exit = getOpenExit(exitId, tenantId);
        Settlement settlement = computeSettlement(tenantId, exit);
        EmployeeExitResponse response = toResponse(exit);
        response.setProratedSalary(settlement.proratedSalary());
        response.setOutstandingAdvanceDeduction(settlement.outstandingAdvance());
        response.setNetSettlementAmount(settlement.netAmount());
        return response;
    }

    @Transactional
    public EmployeeExitResponse settle(Long exitId, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        EmployeeExit exit = getOpenExit(exitId, tenantId);
        Settlement settlement = computeSettlement(tenantId, exit);

        exit.setProratedSalary(settlement.proratedSalary());
        exit.setOutstandingAdvanceDeduction(settlement.outstandingAdvance());
        exit.setNetSettlementAmount(settlement.netAmount());
        exit.setStatus("SETTLED");
        exit.setSettledAt(java.time.LocalDateTime.now());
        exit.setSettledBy(actorId);
        EmployeeExit saved = exitRepository.save(exit);

        employeeService.deactivate(exit.getEmployeeId(), actorId, httpRequest);

        Employee employee = employeeRepository.findById(exit.getEmployeeId()).orElse(null);
        auditService.log(actorId, "EMPLOYEE_EXIT_SETTLED",
                "Full & Final Settlement processed for " + (employee != null ? employee.getEmployeeCode() : exit.getEmployeeId())
                        + " - net settlement " + settlement.netAmount() + " - employee deactivated", httpRequest);

        return toResponse(saved);
    }

    private EmployeeExit getOpenExit(Long exitId, Long tenantId) {
        EmployeeExit exit = exitRepository.findByIdAndClientCompanyId(exitId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Exit record not found: " + exitId));
        if (!"INITIATED".equals(exit.getStatus())) {
            throw new BadRequestException("This exit has already been settled.");
        }
        return exit;
    }

    private Settlement computeSettlement(Long tenantId, EmployeeExit exit) {
        LocalDate lastDay = exit.getLastWorkingDay();

        EmployeeSalaryStructure assignment = employeeSalaryStructureRepository
                .findEffectiveOn(tenantId, exit.getEmployeeId(), lastDay)
                .orElseThrow(() -> new BadRequestException("No salary structure was effective on " + lastDay + " for this employee - cannot compute a settlement."));

        BigDecimal monthlyGross = salaryStructureService.findById(assignment.getSalaryStructureId()).getGrossEarnings();

        int totalDaysInMonth = lastDay.lengthOfMonth();
        int daysWorked = lastDay.getDayOfMonth();
        BigDecimal proratedSalary = monthlyGross
                .multiply(BigDecimal.valueOf(daysWorked))
                .divide(BigDecimal.valueOf(totalDaysInMonth), 2, RoundingMode.HALF_UP);

        BigDecimal outstandingAdvance = employeeAdvanceService.getOutstandingForEmployee(tenantId, exit.getEmployeeId());
        BigDecimal netAmount = proratedSalary.subtract(outstandingAdvance);

        return new Settlement(proratedSalary, outstandingAdvance, netAmount);
    }

    private record Settlement(BigDecimal proratedSalary, BigDecimal outstandingAdvance, BigDecimal netAmount) {}

    private EmployeeExitResponse toResponse(EmployeeExit exit) {
        EmployeeExitResponse response = new EmployeeExitResponse();
        response.setId(exit.getId());
        response.setEmployeeId(exit.getEmployeeId());
        employeeRepository.findById(exit.getEmployeeId()).ifPresent(e -> {
            response.setEmployeeCode(e.getEmployeeCode());
            response.setEmployeeName(e.getFirstName() + " " + e.getLastName());
        });
        response.setResignationDate(exit.getResignationDate());
        response.setLastWorkingDay(exit.getLastWorkingDay());
        response.setNoticePeriodDays(ChronoUnit.DAYS.between(exit.getResignationDate(), exit.getLastWorkingDay()));
        response.setReason(exit.getReason());
        response.setStatus(exit.getStatus());
        response.setProratedSalary(exit.getProratedSalary());
        response.setOutstandingAdvanceDeduction(exit.getOutstandingAdvanceDeduction());
        response.setNetSettlementAmount(exit.getNetSettlementAmount());
        response.setSettledAt(exit.getSettledAt());
        return response;
    }
}
