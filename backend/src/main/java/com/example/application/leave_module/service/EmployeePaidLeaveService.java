package com.example.application.leave_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.leave_module.dto.EmployeeLeaveSummaryResponse;
import com.example.application.leave_module.dto.EmployeePaidLeaveBalanceResponse;
import com.example.application.leave_module.dto.ExtraPaidLeaveRequest;
import com.example.application.leave_module.dto.ExtraPaidLeaveResponse;
import com.example.application.leave_module.entity.EmployeeExtraPaidLeave;
import com.example.application.leave_module.entity.EmployeePaidLeaveBalance;
import com.example.application.leave_module.entity.PaidLeaveConfiguration;
import com.example.application.leave_module.repository.EmployeeExtraPaidLeaveRepository;
import com.example.application.leave_module.repository.EmployeePaidLeaveBalanceRepository;
import com.example.application.login_module.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * The Paid Leave "foundation" (spec section 15): everything future
 * Attendance/Leave-Application/Payroll modules will call instead of
 * touching these tables directly - getEmployeeLeaveBalance,
 * getEmployeeLeaveHistory, getAvailablePaidLeave, plus recordUsage() for
 * whichever module actually knows how much leave was taken.
 *
 * Monthly allocation, carry-forward (with an optional cap), and extra
 * leave are recomputed fresh every time resolveMonth() runs for a given
 * employee+month - re-processing the same month never double-allocates.
 * Policy is resolved per-month via LeavePolicyResolver (architecture
 * refactor Phase 9), never "today's" config, so changing the tenant's
 * policy only ever affects months from its effective date onward -
 * already-generated historical rows are snapshots, not live references,
 * and even a not-yet-generated historical month resolves using whatever
 * policy was actually in effect for it, not today's.
 */
@Service
public class EmployeePaidLeaveService {

    private final EmployeePaidLeaveBalanceRepository balanceRepository;
    private final EmployeeExtraPaidLeaveRepository extraLeaveRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeavePolicyResolver leavePolicyResolver;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    private static final Set<String> VALID_REASONS = Set.of("MEDICAL", "SPECIAL", "EMERGENCY", "OTHER");

    public EmployeePaidLeaveService(EmployeePaidLeaveBalanceRepository balanceRepository,
                                     EmployeeExtraPaidLeaveRepository extraLeaveRepository,
                                     EmployeeRepository employeeRepository,
                                     UserRepository userRepository,
                                     LeavePolicyResolver leavePolicyResolver,
                                     TenantContextService tenantContext,
                                     AuditService auditService) {
        this.balanceRepository = balanceRepository;
        this.extraLeaveRepository = extraLeaveRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.leavePolicyResolver = leavePolicyResolver;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------
    // Reusable methods for future modules (spec section 15)
    // ------------------------------------------------------------------

    /** Resolves (generating if needed) the given employee's balance as of a specific date - reusable by any future consumer. */
    @Transactional
    public EmployeePaidLeaveBalanceResponse getEmployeeLeaveBalance(Long employeeId, LocalDate date) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        validateEmployeeInTenant(tenantId, employeeId);
        assertReadAccess(employeeId);
        return toResponse(resolveMonth(tenantId, employeeId, date.getYear(), date.getMonthValue()));
    }

    @Transactional(readOnly = true)
    public List<EmployeePaidLeaveBalanceResponse> getEmployeeLeaveHistory(Long employeeId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        validateEmployeeInTenant(tenantId, employeeId);
        assertReadAccess(employeeId);
        return balanceRepository.findAllByClientCompanyIdAndEmployeeIdOrderByYearDescMonthDesc(tenantId, employeeId)
                .stream().map(this::toResponse).toList();
    }

    /** Convenience wrapper for callers that only need the number, e.g. a future Leave Application module checking eligibility. */
    @Transactional
    public BigDecimal getAvailablePaidLeave(Long employeeId, LocalDate date) {
        return getEmployeeLeaveBalance(employeeId, date).getAvailableLeave();
    }

    /**
     * Read-only counterpart to resolveMonth() (architecture refactor Phase 4) -
     * returns what this month's paid-leave figures ARE (if already calculated
     * via a Payroll Run or a prior report) or WOULD BE (if not), without ever
     * creating or modifying a balance row. This is what
     * attendance_module.MonthlyAttendanceReportService uses so that opening
     * the Monthly Attendance Report never mutates Leave data - only an
     * explicit Payroll Run calculation (via resolveMonth/recordUsage) commits
     * anything.
     */
    @Transactional(readOnly = true)
    public EmployeePaidLeaveBalanceResponse previewMonth(Long employeeId, int year, int month) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        validateEmployeeInTenant(tenantId, employeeId);
        return balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(tenantId, employeeId, year, month)
                .map(this::toResponse)
                .orElseGet(() -> {
                    PaidLeaveConfiguration config = leavePolicyResolver.resolve(tenantId, year, month);
                    BigDecimal monthlyAllocation = BigDecimal.valueOf(config.getEffectiveMonthlyPaidLeave());
                    BigDecimal carryForward = resolveCarryForward(tenantId, employeeId, year, month, config);
                    BigDecimal extraLeave = sumExtraLeaveGrantedInMonth(tenantId, employeeId, year, month);
                    BigDecimal available = monthlyAllocation.add(carryForward).add(extraLeave);
                    return new EmployeePaidLeaveBalanceResponse(year, month, monthlyAllocation, carryForward, extraLeave,
                            BigDecimal.ZERO, available, false);
                });
    }

    /** Every active employee's current total available paid leave, for the Paid Leave Settings admin overview. */
    @Transactional
    public List<EmployeeLeaveSummaryResponse> listAllEmployeeBalances() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        LocalDate today = LocalDate.now();
        List<Employee> employees = employeeRepository.findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(tenantId, "ACTIVE");
        return employees.stream().map(e -> {
            EmployeePaidLeaveBalance balance = resolveMonth(tenantId, e.getId(), today.getYear(), today.getMonthValue());
            EmployeeLeaveSummaryResponse r = new EmployeeLeaveSummaryResponse();
            r.setEmployeeId(e.getId());
            r.setEmployeeCode(e.getEmployeeCode());
            r.setEmployeeName((e.getFirstName() + " " + e.getLastName()).trim());
            r.setAvailableLeave(balance.getAvailableLeave());
            return r;
        }).toList();
    }

    /**
     * Idempotent: fetch-or-create the balance row for employee+month, and
     * always refresh monthlyAllocation/carryForward/extraLeave from the
     * current config/grants (usedLeave is preserved unless it now exceeds
     * the recomputed available total and hasn't been manually set).
     */
    @Transactional
    public EmployeePaidLeaveBalance resolveMonth(Long tenantId, Long employeeId, int year, int month) {
        EmployeePaidLeaveBalance entry = balanceRepository
                .findByClientCompanyIdAndEmployeeIdAndYearAndMonth(tenantId, employeeId, year, month)
                .orElseGet(() -> {
                    EmployeePaidLeaveBalance created = new EmployeePaidLeaveBalance();
                    created.setClientCompanyId(tenantId);
                    created.setEmployeeId(employeeId);
                    created.setYear(year);
                    created.setMonth(month);
                    return created;
                });

        PaidLeaveConfiguration config = leavePolicyResolver.resolve(tenantId, year, month);
        BigDecimal monthlyAllocation = BigDecimal.valueOf(config.getEffectiveMonthlyPaidLeave());
        BigDecimal carryForward = resolveCarryForward(tenantId, employeeId, year, month, config);
        BigDecimal extraLeave = sumExtraLeaveGrantedInMonth(tenantId, employeeId, year, month);
        BigDecimal available = monthlyAllocation.add(carryForward).add(extraLeave);

        entry.setMonthlyAllocation(monthlyAllocation);
        entry.setCarryForward(carryForward);
        entry.setExtraLeave(extraLeave);

        BigDecimal usedLeave = entry.getUsedLeave();
        if (!entry.isManualOverride() && usedLeave.compareTo(available) > 0) {
            usedLeave = available; // config shrank since this was last generated - don't let it go negative
        }
        entry.setUsedLeave(usedLeave);
        entry.setAvailableLeave(available.subtract(usedLeave));
        return balanceRepository.save(entry);
    }

    /**
     * Whether the PREVIOUS month's surplus carries into THIS month is governed by whichever
     * policy was active for the PREVIOUS month, not this month's own policy (spec section 4/44
     * CASE 4: turning carry-forward ON in March must not resurrect January/February's already-
     * expired leave, even though March's own policy is now ON - it's February's OFF policy that
     * decided February's surplus doesn't carry, and that decision is final). currentConfig's
     * maximumCarryForward still caps how much THIS month can hold, since that's a property of
     * how much the current policy allows accumulating, not of the source month.
     */
    private BigDecimal resolveCarryForward(Long tenantId, Long employeeId, int year, int month, PaidLeaveConfiguration currentConfig) {
        int prevYear = month == 1 ? year - 1 : year;
        int prevMonth = month == 1 ? 12 : month - 1;
        PaidLeaveConfiguration prevConfig = leavePolicyResolver.resolve(tenantId, prevYear, prevMonth);

        if (!prevConfig.isAllowCarryForward()) return BigDecimal.ZERO;
        // Annual reset: whether December's balance carries into January is December's own policy's call.
        if (prevConfig.isResetAnnually() && month == 1) return BigDecimal.ZERO;

        BigDecimal previousAvailable = balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(tenantId, employeeId, prevYear, prevMonth)
                .map(EmployeePaidLeaveBalance::getAvailableLeave)
                .orElse(BigDecimal.ZERO)
                .max(BigDecimal.ZERO);
        return currentConfig.getMaximumCarryForward() != null
                ? previousAvailable.min(BigDecimal.valueOf(currentConfig.getMaximumCarryForward()))
                : previousAvailable;
    }

    /** Extra leave contributes to exactly the calendar month of its startDate - see EmployeeExtraPaidLeave's class comment. */
    private BigDecimal sumExtraLeaveGrantedInMonth(Long tenantId, Long employeeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return extraLeaveRepository
                .findAllByClientCompanyIdAndEmployeeIdAndStatusAndStartDateBetween(tenantId, employeeId, "ACTIVE", ym.atDay(1), ym.atEndOfMonth())
                .stream().map(EmployeeExtraPaidLeave::getLeaveDays).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Auto-recorded usage from a consuming module (e.g. the Monthly Attendance & Payment Report) - a no-op if this month has a manual correction. */
    @Transactional
    public EmployeePaidLeaveBalance recordUsage(Long tenantId, Long employeeId, int year, int month, BigDecimal usedDays) {
        EmployeePaidLeaveBalance entry = resolveMonth(tenantId, employeeId, year, month);
        if (entry.isManualOverride()) return entry;
        entry.setUsedLeave(usedDays);
        entry.setAvailableLeave(entry.getMonthlyAllocation().add(entry.getCarryForward()).add(entry.getExtraLeave()).subtract(usedDays));
        return balanceRepository.save(entry);
    }

    /** Admin's direct correction (e.g. from the Monthly Report table's inline edit). */
    @Transactional
    public void setManualUsage(Long tenantId, Long employeeId, int year, int month, BigDecimal usedDays, Long actorId, HttpServletRequest httpRequest) {
        if (usedDays.signum() < 0) {
            throw new BadRequestException("Used leave cannot be negative");
        }
        EmployeePaidLeaveBalance entry = balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(tenantId, employeeId, year, month)
                .orElseThrow(() -> new BadRequestException("Generate this month's balance first (view the employee's leave or the Monthly Report)"));
        entry.setManualOverride(true);
        entry.setUsedLeave(usedDays);
        entry.setAvailableLeave(entry.getMonthlyAllocation().add(entry.getCarryForward()).add(entry.getExtraLeave()).subtract(usedDays));
        balanceRepository.save(entry);
        auditService.log(actorId, "PAID_LEAVE_UPDATED",
                "Used leave manually set to " + usedDays + " day(s) for employee " + employeeId + " (" + year + "-" + month + ")", httpRequest);
    }

    /** Reverts a month back to auto-recorded usage; the next recordUsage() call (from whichever module tracks actual usage) supplies the fresh figure. */
    @Transactional
    public void clearManualUsage(Long tenantId, Long employeeId, int year, int month, Long actorId, HttpServletRequest httpRequest) {
        EmployeePaidLeaveBalance entry = balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(tenantId, employeeId, year, month)
                .orElseThrow(() -> new BadRequestException("Generate this month's balance first (view the employee's leave or the Monthly Report)"));
        entry.setManualOverride(false);
        balanceRepository.save(entry);
        auditService.log(actorId, "PAID_LEAVE_UPDATED",
                "Manual usage correction cleared for employee " + employeeId + " (" + year + "-" + month + ")", httpRequest);
    }

    // ------------------------------------------------------------------
    // Extra Paid Leave grants (spec sections 3, 4, 6)
    // ------------------------------------------------------------------

    @Transactional
    public ExtraPaidLeaveResponse grantExtraLeave(Long employeeId, ExtraPaidLeaveRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = validateEmployeeInTenant(tenantId, employeeId);
        validateRequest(request);

        EmployeeExtraPaidLeave grant = new EmployeeExtraPaidLeave();
        grant.setClientCompanyId(tenantId);
        grant.setEmployeeId(employeeId);
        grant.setLeaveDays(request.getLeaveDays());
        grant.setReason(request.getReason().toUpperCase());
        grant.setStartDate(request.getStartDate());
        grant.setEndDate(request.getEndDate());
        grant.setRemark(request.getRemark());
        grant.setCreatedBy(actorId);
        EmployeeExtraPaidLeave saved = extraLeaveRepository.save(grant);

        // Refresh the affected month immediately so a subsequent view reflects it without waiting for the next natural resolveMonth() call.
        resolveMonth(tenantId, employeeId, saved.getStartDate().getYear(), saved.getStartDate().getMonthValue());

        auditService.log(actorId, "EXTRA_PAID_LEAVE_GRANTED",
                "Granted " + saved.getLeaveDays() + " day(s) (" + saved.getReason() + ") to employee " + employee.getEmployeeCode(), httpRequest);
        return toExtraResponse(saved, employee);
    }

    @Transactional
    public ExtraPaidLeaveResponse updateExtraLeave(Long employeeId, Long id, ExtraPaidLeaveRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = validateEmployeeInTenant(tenantId, employeeId);
        EmployeeExtraPaidLeave grant = getGrantForEmployee(tenantId, employeeId, id);
        validateRequest(request);

        LocalDate previousStart = grant.getStartDate();
        grant.setLeaveDays(request.getLeaveDays());
        grant.setReason(request.getReason().toUpperCase());
        grant.setStartDate(request.getStartDate());
        grant.setEndDate(request.getEndDate());
        grant.setRemark(request.getRemark());
        EmployeeExtraPaidLeave saved = extraLeaveRepository.save(grant);

        resolveMonth(tenantId, employeeId, previousStart.getYear(), previousStart.getMonthValue());
        if (previousStart.getYear() != saved.getStartDate().getYear() || previousStart.getMonthValue() != saved.getStartDate().getMonthValue()) {
            resolveMonth(tenantId, employeeId, saved.getStartDate().getYear(), saved.getStartDate().getMonthValue());
        }

        auditService.log(actorId, "EXTRA_PAID_LEAVE_UPDATED",
                "Updated extra paid leave grant #" + saved.getId() + " for employee " + employee.getEmployeeCode(), httpRequest);
        return toExtraResponse(saved, employee);
    }

    @Transactional
    public void cancelExtraLeave(Long employeeId, Long id, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = validateEmployeeInTenant(tenantId, employeeId);
        EmployeeExtraPaidLeave grant = getGrantForEmployee(tenantId, employeeId, id);
        grant.setStatus("CANCELLED");
        extraLeaveRepository.save(grant);
        resolveMonth(tenantId, employeeId, grant.getStartDate().getYear(), grant.getStartDate().getMonthValue());
        auditService.log(actorId, "EXTRA_PAID_LEAVE_CANCELLED",
                "Cancelled extra paid leave grant #" + grant.getId() + " for employee " + employee.getEmployeeCode(), httpRequest);
    }

    @Transactional(readOnly = true)
    public List<ExtraPaidLeaveResponse> listExtraLeaveHistory(Long employeeId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = validateEmployeeInTenant(tenantId, employeeId);
        assertReadAccess(employeeId);
        return extraLeaveRepository.findAllByClientCompanyIdAndEmployeeIdOrderByStartDateDesc(tenantId, employeeId)
                .stream().map(g -> toExtraResponse(g, employee)).toList();
    }

    /**
     * Spec section 10: an employee may view only their own Paid Leave
     * information. Anyone holding PAID_LEAVE_READ (CLIENT_ADMIN/SUPER_ADMIN)
     * can view any employee's; everyone else may only view the employee
     * record linked to their own login.
     */
    private void assertReadAccess(Long employeeId) {
        if (tenantContext.currentPermissionNames().contains("PAID_LEAVE_READ")) {
            return;
        }
        Long callerUserId = tenantContext.currentPrincipal().getId();
        boolean isSelf = employeeRepository.findByUserId(callerUserId)
                .map(Employee::getId)
                .map(employeeId::equals)
                .orElse(false);
        if (!isSelf) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to view this employee's paid leave information");
        }
    }

    private void validateRequest(ExtraPaidLeaveRequest request) {
        if (!VALID_REASONS.contains(request.getReason().toUpperCase())) {
            throw new BadRequestException("reason must be one of MEDICAL, SPECIAL, EMERGENCY, OTHER");
        }
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
    }

    private EmployeeExtraPaidLeave getGrantForEmployee(Long tenantId, Long employeeId, Long id) {
        EmployeeExtraPaidLeave grant = extraLeaveRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Extra paid leave grant " + id + " does not belong to the current tenant"));
        if (!grant.getEmployeeId().equals(employeeId)) {
            throw new TenantAccessDeniedException("Extra paid leave grant " + id + " does not belong to employee " + employeeId);
        }
        return grant;
    }

    private Employee validateEmployeeInTenant(Long tenantId, Long employeeId) {
        return employeeRepository.findByIdAndClientCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Employee " + employeeId + " does not belong to the current tenant"));
    }

    private EmployeePaidLeaveBalanceResponse toResponse(EmployeePaidLeaveBalance b) {
        return new EmployeePaidLeaveBalanceResponse(b.getYear(), b.getMonth(), b.getMonthlyAllocation(), b.getCarryForward(),
                b.getExtraLeave(), b.getUsedLeave(), b.getAvailableLeave(), b.isManualOverride());
    }

    private ExtraPaidLeaveResponse toExtraResponse(EmployeeExtraPaidLeave g, Employee employee) {
        ExtraPaidLeaveResponse r = new ExtraPaidLeaveResponse();
        r.setId(g.getId());
        r.setEmployeeId(g.getEmployeeId());
        r.setEmployeeCode(employee.getEmployeeCode());
        r.setEmployeeName((employee.getFirstName() + " " + employee.getLastName()).trim());
        r.setLeaveDays(g.getLeaveDays());
        r.setReason(g.getReason());
        r.setStartDate(g.getStartDate());
        r.setEndDate(g.getEndDate());
        r.setRemark(g.getRemark());
        r.setStatus(g.getStatus());
        r.setCreatedAt(g.getCreatedAt());
        r.setGrantedBy(g.getCreatedBy() == null ? "-" : userRepository.findById(g.getCreatedBy()).map(u -> u.getUsername()).orElse("-"));
        return r;
    }
}
