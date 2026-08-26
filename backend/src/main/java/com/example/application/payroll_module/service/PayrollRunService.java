package com.example.application.payroll_module.service;

import com.example.application.attendance_module.entity.Attendance;
import com.example.application.attendance_module.repository.AttendanceRepository;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.entity.EmployeeSiteAssignment;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.payroll_module.dto.EmployeePayrollInputs;
import com.example.application.payroll_module.dto.PayrollCalculationInput;
import com.example.application.payroll_module.dto.PayrollCalculationResult;
import com.example.application.payroll_module.dto.PayrollRunCreateRequest;
import com.example.application.payroll_module.dto.PayrollRunEmployeeResponse;
import com.example.application.payroll_module.dto.PayrollRunResponse;
import com.example.application.payroll_module.dto.PayrollRunSummaryResponse;
import com.example.application.payroll_module.entity.EmployeePayrollAdjustment;
import com.example.application.payroll_module.entity.PayrollRun;
import com.example.application.payroll_module.entity.PayrollRunEmployee;
import com.example.application.payroll_module.entity.PayrollSettings;
import com.example.application.payroll_module.repository.EmployeePayrollAdjustmentRepository;
import com.example.application.payroll_module.repository.PayrollRunEmployeeRepository;
import com.example.application.payroll_module.repository.PayrollRunRepository;
import com.example.application.salary_structure_module.dto.SalaryStructureResponse;
import com.example.application.site_module.entity.Site;
import com.example.application.site_module.repository.SiteRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The persisted Payroll Run workflow (architecture refactor Phase 2):
 * DRAFT (created, empty) -> CALCULATED (every active employee processed
 * and snapshotted) -> APPROVED -> PAID, with CANCELLED as a separate
 * terminal state reachable from DRAFT/CALCULATED only.
 *
 * Reuses, rather than duplicates, everything Phase 1 already established:
 * PayrollInputResolver gathers each employee's attendance/leave/salary-
 * structure facts (the same class the legacy Monthly Attendance & Payment
 * Report uses), and PayrollCalculationService remains the ONLY place
 * deductions/Net Pay are computed. This class is orchestration only - it
 * contains no salary formula of its own.
 *
 * Once calculate() has run, GET endpoints are pure reads of
 * PayrollRun/PayrollRunEmployee - viewing a payroll run never re-derives
 * numbers, never re-touches Paid Leave or Advance data. Recalculation is
 * only possible while status is still DRAFT or CALCULATED; APPROVED/PAID/
 * CANCELLED runs reject it outright (spec section 8/19/21).
 */
@Service
public class PayrollRunService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunEmployeeRepository payrollRunEmployeeRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeSiteAssignmentRepository siteAssignmentRepository;
    private final SiteRepository siteRepository;
    private final PayrollSettingsResolver payrollSettingsResolver;
    private final EmployeePayrollAdjustmentRepository payrollAdjustmentRepository;
    private final PayrollInputResolver payrollInputResolver;
    private final PayrollCalculationService payrollCalculationService;
    private final PayrollStatusTransitionService statusTransitionService;
    private final UserRepository userRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public PayrollRunService(PayrollRunRepository payrollRunRepository,
                              PayrollRunEmployeeRepository payrollRunEmployeeRepository,
                              EmployeeRepository employeeRepository,
                              AttendanceRepository attendanceRepository,
                              EmployeeSiteAssignmentRepository siteAssignmentRepository,
                              SiteRepository siteRepository,
                              PayrollSettingsResolver payrollSettingsResolver,
                              EmployeePayrollAdjustmentRepository payrollAdjustmentRepository,
                              PayrollInputResolver payrollInputResolver,
                              PayrollCalculationService payrollCalculationService,
                              PayrollStatusTransitionService statusTransitionService,
                              UserRepository userRepository,
                              TenantContextService tenantContext,
                              AuditService auditService) {
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRunEmployeeRepository = payrollRunEmployeeRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.siteAssignmentRepository = siteAssignmentRepository;
        this.siteRepository = siteRepository;
        this.payrollSettingsResolver = payrollSettingsResolver;
        this.payrollAdjustmentRepository = payrollAdjustmentRepository;
        this.payrollInputResolver = payrollInputResolver;
        this.payrollCalculationService = payrollCalculationService;
        this.statusTransitionService = statusTransitionService;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Transactional
    public PayrollRunResponse createRun(PayrollRunCreateRequest request, Long actorId, HttpServletRequest httpRequest) {
        int year = request.getYear();
        int month = request.getMonth();
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        Long tenantId = tenantContext.requireCurrentTenantId();

        // Duplicate-run guard: one non-cancelled PayrollRun per client+month. A cancelled run
        // does not block a new one - reprocessing a cancelled month is a later reopen-workflow
        // phase, not this one, so recreating from scratch here is the safe minimum for now.
        List<PayrollRun> existing = payrollRunRepository.findAllByClientCompanyIdAndYearAndMonthAndStatusNot(tenantId, year, month, "CANCELLED");
        if (!existing.isEmpty()) {
            throw new DuplicateResourceException(
                    "A payroll run for " + monthLabel(year, month) + " already exists (status: " + existing.get(0).getStatus() + ")");
        }

        PayrollRun run = new PayrollRun();
        run.setClientCompanyId(tenantId);
        run.setYear(year);
        run.setMonth(month);
        run.setStatus("DRAFT");
        run.setRemarks(request.getRemarks());
        run.setCreatedBy(actorId);
        PayrollRun saved = payrollRunRepository.save(run);

        auditService.log(actorId, "PAYROLL_RUN_CREATED", "Created payroll run for " + monthLabel(year, month), httpRequest);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Calculate
    // ------------------------------------------------------------------

    /**
     * Not read-only: this is the ONLY endpoint that is allowed to write
     * leave/advance/payroll data - PayrollInputResolver and
     * PayrollCalculationService both upsert supporting records
     * (leave-balance rows, advance-recovery transactions) as a documented
     * part of resolving this month, exactly as they already did for the
     * legacy report. GET endpoints never call this.
     */
    @Transactional
    public PayrollRunResponse calculateRun(Long runId, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollRun run = getRunForTenant(tenantId, runId);

        statusTransitionService.assertCalculable(run.getStatus());

        YearMonth yearMonth = YearMonth.of(run.getYear(), run.getMonth());
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        int daysInMonth = yearMonth.lengthOfMonth();

        List<Employee> employees = employeeRepository.findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(tenantId, "ACTIVE");
        if (employees.isEmpty()) {
            throw new BadRequestException("No active employees found for this tenant - nothing to calculate");
        }

        Map<Long, List<Attendance>> attendanceByEmployee = attendanceRepository
                .findAllByClientCompanyIdAndAttendanceDateBetweenOrderByEmployeeIdAscAttendanceDateAsc(tenantId, monthStart, monthEnd)
                .stream().collect(Collectors.groupingBy(Attendance::getEmployeeId));

        Map<Long, String> siteNameById = siteRepository.findAllByClientCompanyId(tenantId).stream()
                .collect(Collectors.toMap(Site::getId, Site::getSiteName));

        Map<Long, EmployeeSiteAssignment> currentSiteByEmployee = new HashMap<>();
        for (EmployeeSiteAssignment a : siteAssignmentRepository.findAllByClientCompanyIdAndStatus(tenantId, "ACTIVE")) {
            currentSiteByEmployee.merge(a.getEmployeeId(), a,
                    (existingAssignment, candidate) -> (candidate.isPrimary() && !existingAssignment.isPrimary()) ? candidate : existingAssignment);
        }

        Map<Long, SalaryStructureResponse> structureCache = new HashMap<>();
        PayrollSettings payrollSettings = payrollSettingsResolver.resolve(tenantId, run.getYear(), run.getMonth());
        Map<Long, EmployeePayrollAdjustment> adjustmentByEmployee = payrollAdjustmentRepository
                .findAllByClientCompanyIdAndYearAndMonth(tenantId, run.getYear(), run.getMonth())
                .stream().collect(Collectors.toMap(EmployeePayrollAdjustment::getEmployeeId, a -> a));

        for (Employee e : employees) {
            List<Attendance> marked = attendanceByEmployee.getOrDefault(e.getId(), List.of());
            EmployeePayrollInputs in = payrollInputResolver.resolveEmployeeInputs(
                    tenantId, e, run.getYear(), run.getMonth(), monthEnd, daysInMonth, marked, structureCache);

            EmployeePayrollAdjustment adjustment = adjustmentByEmployee.get(e.getId());
            BigDecimal manualDeduction = adjustment != null ? adjustment.getOtherManualDeduction() : BigDecimal.ZERO;
            BigDecimal manualAllowance = adjustment != null ? adjustment.getAllowance() : BigDecimal.ZERO;

            PayrollCalculationInput calcInput = PayrollCalculationInput.builder()
                    .tenantId(tenantId).employeeId(e.getId()).year(run.getYear()).month(run.getMonth()).payrollRunId(run.getId())
                    .basicSalary(in.getBasicSalary()).da(in.getDa()).totalGross(in.getTotalGross())
                    .pf(payrollSettings.isEpfEnabled() && e.isPfApplicable(), payrollSettings.getEpfEmployeePercent(), payrollSettings.getEpfEmployerPercent())
                    .esi(payrollSettings.isEsiEnabled() && e.isEsiApplicable(), payrollSettings.getEsiEmployeePercent(), payrollSettings.getEsiEmployerPercent(), payrollSettings.getEsiWageCeiling())
                    .pt(payrollSettings.isPtEnabled() && e.isPtApplicable(), payrollSettings.getProfessionalTax())
                    .otherManualDeduction(manualDeduction).allowance(manualAllowance)
                    .build();

            PayrollCalculationResult result = payrollCalculationService.calculate(calcInput);

            EmployeeSiteAssignment assignment = currentSiteByEmployee.get(e.getId());
            String siteName = assignment != null ? siteNameById.getOrDefault(assignment.getSiteId(), "Unknown Site") : "Unassigned";

            // Idempotent: recalculating the same (still-editable) run updates this employee's
            // existing row instead of inserting a second one - "one PayrollRunEmployee per
            // PayrollRun + Employee" holds even across repeated calculation.
            PayrollRunEmployee pre = payrollRunEmployeeRepository.findByPayrollRunIdAndEmployeeId(run.getId(), e.getId())
                    .orElseGet(() -> {
                        PayrollRunEmployee created = new PayrollRunEmployee();
                        created.setPayrollRunId(run.getId());
                        created.setEmployeeId(e.getId());
                        return created;
                    });

            pre.setEmployeeCode(e.getEmployeeCode());
            pre.setEmployeeName(String.join(" ", Arrays.asList(e.getFirstName(), nullToEmpty(e.getMiddleName()), e.getLastName()))
                    .replaceAll("\\s+", " ").trim());
            pre.setDepartment(e.getDepartment());
            pre.setDesignation(e.getDesignation());
            pre.setSiteName(siteName);
            pre.setSalaryStructureName(in.getStructureName());
            pre.setSalaryType(in.getSalaryType());

            pre.setTotalCalendarDays(daysInMonth);
            pre.setPresentDays((int) in.getPresentDays());
            pre.setHalfDays((int) in.getHalfDays());
            pre.setOnLeaveDays((int) in.getOnLeaveDays());
            pre.setAbsentDays((int) in.getAbsentDays());
            pre.setPaidLeaveDays(in.getPaidLeaveDays());
            pre.setUnpaidLeaveDays(in.getUnpaidLeaveDays());
            pre.setPayableDays(in.getPayableDays());
            pre.setLeaveBalanceClosing(in.getLeaveBalanceClosing());

            pre.setBasicSalary(result.getBasicSalary());
            pre.setDa(result.getDa());
            pre.setGrossSalary(result.getTotalGross());

            pre.setAllowance(result.getAllowance());
            pre.setTotalEarnings(result.getTotalGross().add(result.getAllowance()));

            pre.setEpfEmployee(result.getEpfEmployee());
            pre.setEpfEmployer(result.getEpfEmployer());
            pre.setEsiEmployee(result.getEsiEmployee());
            pre.setEsiEmployer(result.getEsiEmployer());

            // Rate snapshot (architecture refactor Phase 8) - only set when the deduction was
            // actually applied this month, so a payslip can show "12% of 15,000 = 1,800" even
            // after the tenant's current PF rate has since changed - null (not "0%") when the
            // deduction genuinely didn't apply, so the two cases are never confused.
            boolean pfWasApplied = result.getEpfEmployee().signum() > 0 || result.getEpfEmployer().signum() > 0;
            pre.setEpfEmployeePercentUsed(pfWasApplied ? payrollSettings.getEpfEmployeePercent() : null);
            pre.setEpfEmployerPercentUsed(pfWasApplied ? payrollSettings.getEpfEmployerPercent() : null);
            boolean esiWasApplied = result.getEsiEmployee().signum() > 0 || result.getEsiEmployer().signum() > 0;
            pre.setEsiEmployeePercentUsed(esiWasApplied ? payrollSettings.getEsiEmployeePercent() : null);
            pre.setEsiEmployerPercentUsed(esiWasApplied ? payrollSettings.getEsiEmployerPercent() : null);

            pre.setProfessionalTax(result.getProfessionalTax());
            pre.setOtherManualDeduction(result.getOtherManualDeduction());
            pre.setAdvanceRecovery(result.getAdvanceRecovery());
            pre.setTotalDeductions(result.getTotalDeduct());

            pre.setAdvanceOutstandingAfterRecovery(result.getOutstandingAdvance());
            pre.setAdvanceOutstandingBeforeRecovery(result.getOutstandingAdvance().add(result.getAdvanceRecovery()));

            pre.setTotalSalaryCtc(result.getTotalSalary());
            pre.setNetPay(result.getNetPayment());
            pre.setNote(in.getNote());

            payrollRunEmployeeRepository.save(pre);
        }

        run.setStatus("CALCULATED");
        run.setCalculatedAt(LocalDateTime.now());
        run.setCalculatedBy(actorId);
        PayrollRun saved = payrollRunRepository.save(run);

        auditService.log(actorId, "PAYROLL_RUN_CALCULATED",
                "Calculated payroll run for " + monthLabel(run.getYear(), run.getMonth()) + " (" + employees.size() + " employee(s))", httpRequest);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Status transitions
    // ------------------------------------------------------------------

    @Transactional
    public PayrollRunResponse approveRun(Long runId, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollRun run = getRunForTenant(tenantId, runId);
        statusTransitionService.assertApprovable(run.getStatus());

        // Approval validation (spec section 21/22) - the DB unique constraint on
        // (payroll_run_id, employee_id) already rules out duplicate employees, and the
        // summary is always derived live from these same rows (never stored separately), so
        // it can never be inconsistent with them - both checks are structural, not runtime.
        List<PayrollRunEmployee> rows = payrollRunEmployeeRepository.findAllByPayrollRunIdOrderByEmployeeCodeAsc(run.getId());
        if (rows.isEmpty()) {
            throw new BadRequestException("This payroll run has no calculated employee results - nothing to approve");
        }
        List<String> negativeNetPay = rows.stream()
                .filter(r -> r.getNetPay() != null && r.getNetPay().signum() < 0)
                .map(PayrollRunEmployee::getEmployeeCode)
                .toList();
        if (!negativeNetPay.isEmpty()) {
            throw new BadRequestException("Cannot approve - Net Pay is negative for: " + String.join(", ", negativeNetPay)
                    + ". Correct the underlying deductions/adjustments and recalculate before approving.");
        }

        run.setStatus("APPROVED");
        run.setApprovedAt(LocalDateTime.now());
        run.setApprovedBy(actorId);
        PayrollRun saved = payrollRunRepository.save(run);
        auditService.log(actorId, "PAYROLL_RUN_APPROVED", "Approved payroll run for " + monthLabel(run.getYear(), run.getMonth()), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public PayrollRunResponse markPaid(Long runId, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollRun run = getRunForTenant(tenantId, runId);
        statusTransitionService.assertPayable(run.getStatus());
        run.setStatus("PAID");
        run.setPaidAt(LocalDateTime.now());
        run.setPaidBy(actorId);
        PayrollRun saved = payrollRunRepository.save(run);
        auditService.log(actorId, "PAYROLL_RUN_PAID", "Marked payroll run for " + monthLabel(run.getYear(), run.getMonth()) + " as paid", httpRequest);
        return toResponse(saved);
    }

    /** Cancellation reason is mandatory (spec section 10) - unlike the other transitions, a cancellation with no explanation is not acceptable audit trail. */
    @Transactional
    public PayrollRunResponse cancelRun(Long runId, String cancellationReason, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollRun run = getRunForTenant(tenantId, runId);
        statusTransitionService.assertCancellable(run.getStatus());
        if (cancellationReason == null || cancellationReason.isBlank()) {
            throw new BadRequestException("A cancellation reason is required");
        }
        run.setStatus("CANCELLED");
        run.setCancelledAt(LocalDateTime.now());
        run.setCancelledBy(actorId);
        run.setCancellationReason(cancellationReason);
        PayrollRun saved = payrollRunRepository.save(run);
        auditService.log(actorId, "PAYROLL_RUN_CANCELLED",
                "Cancelled payroll run for " + monthLabel(run.getYear(), run.getMonth()) + " - reason: " + cancellationReason, httpRequest);
        return toResponse(saved);
    }

    /**
     * Controlled reopen (spec section 11) - APPROVED -> CALCULATED only, since the run already
     * has calculated employee results that just need re-review/recalculation, not a return to
     * an empty DRAFT. PAID payroll is explicitly rejected with its own distinct message (spec
     * section 12) - never silently reopened. Gated by its own PAYROLL_RUN_REOPEN permission
     * (see PayrollRunController), separate from ordinary calculate/approve, since reopening an
     * already-approved payroll is a more sensitive action than the normal forward workflow.
     */
    @Transactional
    public PayrollRunResponse reopenRun(Long runId, String reopenReason, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollRun run = getRunForTenant(tenantId, runId);
        statusTransitionService.assertReopenable(run.getStatus());
        if (reopenReason == null || reopenReason.isBlank()) {
            throw new BadRequestException("A reopen reason is required");
        }
        // approvedAt/approvedBy are deliberately left as-is (not cleared) - they remain a
        // permanent record that this run WAS approved once, by whom; reopenedAt/reopenedBy/
        // reopenReason record the separate, later reopen event alongside that, not instead of it.
        run.setStatus("CALCULATED");
        run.setReopenedAt(LocalDateTime.now());
        run.setReopenedBy(actorId);
        run.setReopenReason(reopenReason);
        PayrollRun saved = payrollRunRepository.save(run);
        auditService.log(actorId, "PAYROLL_RUN_REOPENED",
                "Reopened payroll run for " + monthLabel(run.getYear(), run.getMonth()) + " - reason: " + reopenReason, httpRequest);
        return toResponse(saved);
    }

    /**
     * Sets the manual Advance/Uniform deduction and Allowance for one employee within this
     * run's month (architecture refactor Phase 4 - this used to live on the Monthly Attendance
     * Report; adjustments are a Payroll concern, so they're set here instead, and only take
     * effect the next time this run is calculated). Only allowed while the run is still
     * DRAFT/CALCULATED - an APPROVED/PAID run's figures are frozen, same rule as recalculation.
     */
    @Transactional
    public void setEmployeeAdjustment(Long runId, Long employeeId, BigDecimal otherManualDeduction, BigDecimal allowance,
                                       Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollRun run = getRunForTenant(tenantId, runId);
        statusTransitionService.assertEditable(run.getStatus());
        if (otherManualDeduction.signum() < 0 || allowance.signum() < 0) {
            throw new BadRequestException("Advance/Uniform deduction and Allowance must be >= 0");
        }
        EmployeePayrollAdjustment adjustment = payrollAdjustmentRepository
                .findByClientCompanyIdAndEmployeeIdAndYearAndMonth(tenantId, employeeId, run.getYear(), run.getMonth())
                .orElseGet(() -> {
                    EmployeePayrollAdjustment created = new EmployeePayrollAdjustment();
                    created.setClientCompanyId(tenantId);
                    created.setEmployeeId(employeeId);
                    created.setYear(run.getYear());
                    created.setMonth(run.getMonth());
                    return created;
                });
        adjustment.setOtherManualDeduction(otherManualDeduction);
        adjustment.setAllowance(allowance);
        payrollAdjustmentRepository.save(adjustment);
        auditService.log(actorId, "PAYROLL_ADJUSTMENT_UPDATED",
                "Set Other Deduction=" + otherManualDeduction + ", Allowance=" + allowance + " for employee " + employeeId
                        + " (" + monthLabel(run.getYear(), run.getMonth()) + ") - applies on next calculation", httpRequest);
    }

    // ------------------------------------------------------------------
    // Read (never recalculates - pure persisted-data reads)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PayrollRunResponse getRun(Long runId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return toResponse(getRunForTenant(tenantId, runId));
    }

    @Transactional(readOnly = true)
    public Page<PayrollRunResponse> listRuns(Integer year, Integer month, String status, Pageable pageable) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Page<PayrollRun> page;
        if (year != null && month != null && status != null) {
            page = payrollRunRepository.findAllByClientCompanyIdAndYearAndMonthAndStatus(tenantId, year, month, status, pageable);
        } else if (year != null && month != null) {
            page = payrollRunRepository.findAllByClientCompanyIdAndYearAndMonth(tenantId, year, month, pageable);
        } else if (year != null && status != null) {
            page = payrollRunRepository.findAllByClientCompanyIdAndYearAndStatus(tenantId, year, status, pageable);
        } else if (year != null) {
            page = payrollRunRepository.findAllByClientCompanyIdAndYear(tenantId, year, pageable);
        } else if (status != null) {
            page = payrollRunRepository.findAllByClientCompanyIdAndStatus(tenantId, status, pageable);
        } else {
            page = payrollRunRepository.findAllByClientCompanyId(tenantId, pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PayrollRunEmployeeResponse> getRunEmployees(Long runId, Pageable pageable) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollRun run = getRunForTenant(tenantId, runId);
        return payrollRunEmployeeRepository.findAllByPayrollRunIdOrderByEmployeeCodeAsc(run.getId(), pageable).map(this::toEmployeeResponse);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private PayrollRun getRunForTenant(Long tenantId, Long runId) {
        return payrollRunRepository.findByIdAndClientCompanyId(runId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run " + runId + " not found"));
    }

    private String monthLabel(int year, int month) {
        return YearMonth.of(year, month).getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private PayrollRunResponse toResponse(PayrollRun run) {
        PayrollRunResponse r = new PayrollRunResponse();
        r.setId(run.getId());
        r.setYear(run.getYear());
        r.setMonth(run.getMonth());
        r.setMonthLabel(monthLabel(run.getYear(), run.getMonth()));
        r.setStatus(run.getStatus());
        r.setRemarks(run.getRemarks());
        r.setCreatedAt(run.getCreatedAt());
        r.setCreatedBy(usernameOf(run.getCreatedBy()));
        r.setCalculatedAt(run.getCalculatedAt());
        r.setCalculatedBy(usernameOf(run.getCalculatedBy()));
        r.setApprovedAt(run.getApprovedAt());
        r.setApprovedBy(usernameOf(run.getApprovedBy()));
        r.setPaidAt(run.getPaidAt());
        r.setPaidBy(usernameOf(run.getPaidBy()));
        r.setCancelledAt(run.getCancelledAt());
        r.setCancelledBy(usernameOf(run.getCancelledBy()));
        r.setCancellationReason(run.getCancellationReason());
        r.setReopenedAt(run.getReopenedAt());
        r.setReopenedBy(usernameOf(run.getReopenedBy()));
        r.setReopenReason(run.getReopenReason());
        r.setSummary(buildSummary(run.getId()));
        return r;
    }

    /** Always derived from persisted PayrollRunEmployee rows at read time - never stored, so it can never drift (spec section 10). */
    private PayrollRunSummaryResponse buildSummary(Long runId) {
        List<PayrollRunEmployee> rows = payrollRunEmployeeRepository.findAllByPayrollRunIdOrderByEmployeeCodeAsc(runId);
        BigDecimal totalGross = sum(rows, PayrollRunEmployee::getGrossSalary);
        BigDecimal totalEarnings = sum(rows, PayrollRunEmployee::getTotalEarnings);
        BigDecimal totalEpf = sum(rows, PayrollRunEmployee::getEpfEmployee);
        BigDecimal totalEsi = sum(rows, PayrollRunEmployee::getEsiEmployee);
        BigDecimal totalPt = sum(rows, PayrollRunEmployee::getProfessionalTax);
        BigDecimal totalOther = sum(rows, PayrollRunEmployee::getOtherManualDeduction);
        BigDecimal totalAdvance = sum(rows, PayrollRunEmployee::getAdvanceRecovery);
        BigDecimal totalDeductions = sum(rows, PayrollRunEmployee::getTotalDeductions);
        BigDecimal totalNetPay = sum(rows, PayrollRunEmployee::getNetPay);
        return new PayrollRunSummaryResponse(rows.size(), totalGross, totalEarnings, totalEpf, totalEsi, totalPt,
                totalOther, totalAdvance, totalDeductions, totalNetPay);
    }

    private BigDecimal sum(List<PayrollRunEmployee> rows, java.util.function.Function<PayrollRunEmployee, BigDecimal> getter) {
        return rows.stream().map(getter).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String usernameOf(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(u -> u.getUsername()).orElse(null);
    }

    private PayrollRunEmployeeResponse toEmployeeResponse(PayrollRunEmployee e) {
        PayrollRunEmployeeResponse r = new PayrollRunEmployeeResponse();
        r.setId(e.getId());
        r.setEmployeeId(e.getEmployeeId());
        r.setEmployeeCode(e.getEmployeeCode());
        r.setEmployeeName(e.getEmployeeName());
        r.setDepartment(e.getDepartment());
        r.setDesignation(e.getDesignation());
        r.setSiteName(e.getSiteName());
        r.setSalaryStructureName(e.getSalaryStructureName());
        r.setSalaryType(e.getSalaryType());
        r.setTotalCalendarDays(e.getTotalCalendarDays());
        r.setPresentDays(e.getPresentDays());
        r.setHalfDays(e.getHalfDays());
        r.setOnLeaveDays(e.getOnLeaveDays());
        r.setAbsentDays(e.getAbsentDays());
        r.setPaidLeaveDays(e.getPaidLeaveDays());
        r.setUnpaidLeaveDays(e.getUnpaidLeaveDays());
        r.setPayableDays(e.getPayableDays());
        r.setLeaveBalanceClosing(e.getLeaveBalanceClosing());
        r.setBasicSalary(e.getBasicSalary());
        r.setDa(e.getDa());
        r.setGrossSalary(e.getGrossSalary());
        r.setAllowance(e.getAllowance());
        r.setTotalEarnings(e.getTotalEarnings());
        r.setEpfEmployee(e.getEpfEmployee());
        r.setEpfEmployer(e.getEpfEmployer());
        r.setEpfEmployeePercentUsed(e.getEpfEmployeePercentUsed());
        r.setEpfEmployerPercentUsed(e.getEpfEmployerPercentUsed());
        r.setEsiEmployee(e.getEsiEmployee());
        r.setEsiEmployer(e.getEsiEmployer());
        r.setEsiEmployeePercentUsed(e.getEsiEmployeePercentUsed());
        r.setEsiEmployerPercentUsed(e.getEsiEmployerPercentUsed());
        r.setProfessionalTax(e.getProfessionalTax());
        r.setOtherManualDeduction(e.getOtherManualDeduction());
        r.setAdvanceRecovery(e.getAdvanceRecovery());
        r.setTotalDeductions(e.getTotalDeductions());
        r.setAdvanceOutstandingBeforeRecovery(e.getAdvanceOutstandingBeforeRecovery());
        r.setAdvanceOutstandingAfterRecovery(e.getAdvanceOutstandingAfterRecovery());
        r.setTotalSalaryCtc(e.getTotalSalaryCtc());
        r.setNetPay(e.getNetPay());
        r.setNote(e.getNote());
        return r;
    }
}
