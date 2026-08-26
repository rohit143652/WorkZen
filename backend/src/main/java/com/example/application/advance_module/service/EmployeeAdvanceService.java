package com.example.application.advance_module.service;

import com.example.application.advance_module.dto.AdvanceGrantRequest;
import com.example.application.advance_module.dto.AdvanceDashboardSummaryResponse;
import com.example.application.advance_module.dto.AdvanceRecoveryAmountRequest;
import com.example.application.advance_module.dto.AdvanceRecoveryTransactionResponse;
import com.example.application.advance_module.dto.EmployeeAdvanceResponse;
import com.example.application.advance_module.entity.AdvanceRecoveryTransaction;
import com.example.application.advance_module.entity.EmployeeAdvance;
import com.example.application.advance_module.repository.AdvanceRecoveryTransactionRepository;
import com.example.application.advance_module.repository.EmployeeAdvanceRepository;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Employee Advances (spec sections 12-16): every advance a company hands
 * out is its own row, never overwritten; recovery against it is tracked as
 * individual monthly transactions so changing the recovery amount only
 * affects future months (spec section 15), and manual settlement (section
 * 16) stops further payroll recovery without needing to touch history.
 */
@Service
public class EmployeeAdvanceService {

    private final EmployeeAdvanceRepository advanceRepository;
    private final AdvanceRecoveryTransactionRepository recoveryRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public EmployeeAdvanceService(EmployeeAdvanceRepository advanceRepository,
                                   AdvanceRecoveryTransactionRepository recoveryRepository,
                                   EmployeeRepository employeeRepository,
                                   UserRepository userRepository,
                                   TenantContextService tenantContext,
                                   AuditService auditService) {
        this.advanceRepository = advanceRepository;
        this.recoveryRepository = recoveryRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional
    public EmployeeAdvanceResponse grantAdvance(Long employeeId, AdvanceGrantRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = validateEmployee(tenantId, employeeId);

        EmployeeAdvance advance = new EmployeeAdvance();
        advance.setClientCompanyId(tenantId);
        advance.setEmployeeId(employeeId);
        advance.setAdvanceDate(request.getAdvanceDate());
        advance.setAmount(request.getAmount());
        advance.setReason(request.getReason());
        advance.setPaymentMode(request.getPaymentMode());
        advance.setMonthlyRecoveryAmount(request.getMonthlyRecoveryAmount());
        // Recovery Start Month (spec section 9/10): defaults to the advance date's own month -
        // i.e. recovery normally begins the same month the advance is granted - unless the
        // admin explicitly picks a later starting month.
        advance.setRecoveryStartYear(request.getRecoveryStartYear() != null ? request.getRecoveryStartYear() : request.getAdvanceDate().getYear());
        advance.setRecoveryStartMonth(request.getRecoveryStartMonth() != null ? request.getRecoveryStartMonth() : request.getAdvanceDate().getMonthValue());
        advance.setRemarks(request.getRemarks());
        advance.setCreatedBy(actorId);
        EmployeeAdvance saved = advanceRepository.save(advance);

        auditService.log(actorId, "ADVANCE_GRANTED",
                "Granted advance of " + saved.getAmount() + " to employee " + employee.getEmployeeCode()
                        + " (recovery " + saved.getMonthlyRecoveryAmount() + "/month starting "
                        + saved.getRecoveryStartMonth() + "/" + saved.getRecoveryStartYear() + ")", httpRequest);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeAdvanceResponse> listAdvances(Long employeeId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        validateEmployee(tenantId, employeeId);
        return advanceRepository.findAllByClientCompanyIdAndEmployeeIdOrderByAdvanceDateDesc(tenantId, employeeId)
                .stream().map(this::toResponse).toList();
    }

    /** Every advance across every employee for the tenant, newest first - for the Advance Dashboard (spec section 12/13). */
    @Transactional(readOnly = true)
    public List<EmployeeAdvanceResponse> listAllForTenant() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return advanceRepository.findAllByClientCompanyIdOrderByAdvanceDateDesc(tenantId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Tenant-wide totals for the Advance Dashboard - always computed live from persisted data,
     * never stored separately. Fetches every recovery transaction for this tenant's advances in
     * ONE query (not one query per advance) and sums from that in-memory map - the previous
     * version called recoveryRepository.findAllByAdvanceId() up to three times per advance,
     * meaning a tenant with 50 advances issued ~150 queries just to load this dashboard.
     */
    @Transactional(readOnly = true)
    public AdvanceDashboardSummaryResponse getDashboardSummary() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        List<EmployeeAdvance> advances = advanceRepository.findAllByClientCompanyIdOrderByAdvanceDateDesc(tenantId);

        Map<Long, List<AdvanceRecoveryTransaction>> transactionsByAdvanceId = recoveryRepository
                .findAllByAdvanceIdIn(advances.stream().map(EmployeeAdvance::getId).toList())
                .stream().collect(Collectors.groupingBy(AdvanceRecoveryTransaction::getAdvanceId));

        BigDecimal totalGiven = advances.stream().map(EmployeeAdvance::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRecovered = transactionsByAdvanceId.values().stream()
                .flatMap(List::stream)
                .map(AdvanceRecoveryTransaction::getRecoveredAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = advances.stream().filter(a -> "ACTIVE".equals(a.getStatus()))
                .map(a -> outstandingFrom(a, transactionsByAdvanceId.getOrDefault(a.getId(), List.of())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        BigDecimal currentMonthRecovery = transactionsByAdvanceId.values().stream()
                .flatMap(List::stream)
                .filter(t -> t.getYear() == today.getYear() && t.getMonth() == today.getMonthValue())
                .map(AdvanceRecoveryTransaction::getRecoveredAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdvanceDashboardSummaryResponse(advances.size(), totalGiven, totalRecovered, totalOutstanding, currentMonthRecovery);
    }

    /** Every recovery event for one advance, newest first - answers "which payroll (or manual payment) recovered this amount?" (spec section 10). */
    @Transactional(readOnly = true)
    public List<AdvanceRecoveryTransactionResponse> getRecoveryHistory(Long employeeId, Long advanceId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        getAdvanceForEmployee(tenantId, employeeId, advanceId);
        return recoveryRepository.findAllByAdvanceIdOrderByYearDescMonthDescCreatedAtDesc(advanceId)
                .stream().map(this::toRecoveryResponse).toList();
    }

    @Transactional
    public EmployeeAdvanceResponse updateRecoveryAmount(Long employeeId, Long advanceId, AdvanceRecoveryAmountRequest request,
                                                         Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        EmployeeAdvance advance = getAdvanceForEmployee(tenantId, employeeId, advanceId);
        BigDecimal previous = advance.getMonthlyRecoveryAmount();
        advance.setMonthlyRecoveryAmount(request.getMonthlyRecoveryAmount());
        advance.setUpdatedBy(actorId);
        EmployeeAdvance saved = advanceRepository.save(advance);
        auditService.log(actorId, "ADVANCE_RECOVERY_CHANGED",
                "Advance #" + saved.getId() + " monthly recovery changed from " + previous + " to " + saved.getMonthlyRecoveryAmount()
                        + " - applies from the next month processed onward, past recovery unaffected", httpRequest);
        return toResponse(saved);
    }

    /**
     * Pauses or resumes PAYROLL-sourced recovery for this advance (spec: "option in edit -
     * should this month's payment cut it or not"). Does not touch monthlyRecoveryAmount, status,
     * or any already-recorded recovery transaction - purely gates whether
     * computeMonthlyRecovery() considers this advance the next time payroll is calculated.
     */
    @Transactional
    public EmployeeAdvanceResponse updateRecoverViaPayroll(Long employeeId, Long advanceId, boolean recoverViaPayroll,
                                                             Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        EmployeeAdvance advance = getAdvanceForEmployee(tenantId, employeeId, advanceId);
        advance.setRecoverViaPayroll(recoverViaPayroll);
        advance.setUpdatedBy(actorId);
        EmployeeAdvance saved = advanceRepository.save(advance);
        auditService.log(actorId, "ADVANCE_PAYROLL_RECOVERY_" + (recoverViaPayroll ? "RESUMED" : "PAUSED"),
                "Advance #" + saved.getId() + " payroll-based recovery " + (recoverViaPayroll ? "resumed" : "paused")
                        + " - applies from the next payroll calculation onward", httpRequest);
        return toResponse(saved);
    }

    /** Manual settlement (spec section 16) - stops further payroll recovery without rewriting any recovery already recorded. */
    @Transactional
    public EmployeeAdvanceResponse settleAdvance(Long employeeId, Long advanceId, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        EmployeeAdvance advance = getAdvanceForEmployee(tenantId, employeeId, advanceId);
        if (!"ACTIVE".equals(advance.getStatus())) {
            throw new BadRequestException("Only an active advance can be settled");
        }
        BigDecimal outstanding = getOutstanding(advance);
        advance.setStatus("SETTLED");
        advance.setSettledAmount(outstanding);
        advance.setUpdatedBy(actorId);
        EmployeeAdvance saved = advanceRepository.save(advance);
        auditService.log(actorId, "ADVANCE_SETTLED",
                "Advance #" + saved.getId() + " manually settled (outstanding " + outstanding + " written off outside payroll)", httpRequest);
        return toResponse(saved);
    }

    /**
     * Partial manual settlement (spec section 17) - the employee paid some of the outstanding
     * amount directly, outside payroll. Recorded as a MANUAL_SETTLEMENT AdvanceRecoveryTransaction
     * (payrollRunId = null) so it reduces outstanding through the exact same summation
     * getOutstanding() already uses - no parallel bookkeeping. If the settled amount happens to
     * clear the entire remaining outstanding, the advance is automatically marked SETTLED too
     * (spec section 14: "do not mark the advance settled unless outstanding becomes zero").
     *
     * ALWAYS inserts a new row - every settlement click is its own permanent, independently
     * visible history entry, even if the employee pays twice on the same day (unlike the
     * PAYROLL source, which is intentionally upserted once per calendar month since it's the
     * one recurring, idempotent payroll deduction - a manual settlement has no such "once a
     * month" concept, each one is a distinct real-world payment event).
     */
    @Transactional
    public EmployeeAdvanceResponse settlePartial(Long employeeId, Long advanceId, BigDecimal amount, String remark,
                                                  Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        EmployeeAdvance advance = getAdvanceForEmployee(tenantId, employeeId, advanceId);
        if (!"ACTIVE".equals(advance.getStatus())) {
            throw new BadRequestException("Only an active advance can receive a settlement");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Settlement amount must be greater than 0");
        }
        BigDecimal outstanding = getOutstanding(advance);
        if (amount.compareTo(outstanding) > 0) {
            throw new BadRequestException("Settlement amount (" + amount + ") cannot exceed the outstanding amount (" + outstanding + ")");
        }

        LocalDate today = LocalDate.now();
        AdvanceRecoveryTransaction txn = new AdvanceRecoveryTransaction();
        txn.setClientCompanyId(tenantId);
        txn.setEmployeeId(employeeId);
        txn.setAdvanceId(advance.getId());
        txn.setYear(today.getYear());
        txn.setMonth(today.getMonthValue());
        txn.setSource("MANUAL_SETTLEMENT");
        txn.setRecoveredAmount(amount);
        txn.setCreatedBy(actorId);
        recoveryRepository.save(txn);

        BigDecimal remainingOutstanding = outstanding.subtract(amount);
        if (remainingOutstanding.signum() <= 0) {
            advance.setStatus("SETTLED");
            advance.setSettledAmount(amount);
            advance.setUpdatedBy(actorId);
            advanceRepository.save(advance);
        }

        auditService.log(actorId, "ADVANCE_PARTIALLY_SETTLED",
                "Advance #" + advance.getId() + " received a " + amount + " manual settlement outside payroll"
                        + (remark != null && !remark.isBlank() ? " (" + remark + ")" : "")
                        + " - outstanding now " + remainingOutstanding.max(BigDecimal.ZERO), httpRequest);
        return toResponse(getAdvanceForEmployee(tenantId, employeeId, advanceId));
    }

    @Transactional(readOnly = true)
    public BigDecimal getOutstandingForEmployee(Long tenantId, Long employeeId) {
        List<EmployeeAdvance> advances = advanceRepository.findAllByClientCompanyIdAndEmployeeIdOrderByAdvanceDateDesc(tenantId, employeeId)
                .stream().filter(a -> "ACTIVE".equals(a.getStatus())).toList();
        Map<Long, List<AdvanceRecoveryTransaction>> transactionsByAdvanceId = recoveryRepository
                .findAllByAdvanceIdIn(advances.stream().map(EmployeeAdvance::getId).toList())
                .stream().collect(Collectors.groupingBy(AdvanceRecoveryTransaction::getAdvanceId));
        return advances.stream()
                .map(a -> outstandingFrom(a, transactionsByAdvanceId.getOrDefault(a.getId(), List.of())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Recovers up to maxAllowed this month across all of the employee's ACTIVE advances
     * (oldest first), never letting a single advance's recovery exceed its own remaining
     * outstanding - this is what keeps Net Payment from going negative (spec section 28,
     * Case 3): the caller (PayrollCalculationService) passes whatever room is left in the
     * employee's gross pay after other deductions.
     *
     * Idempotent per (advance, year, month) - recalculating the same still-editable Payroll
     * Run updates these same rows in place rather than inserting duplicates (spec section 31).
     * payrollRunId is stamped onto every row this call touches so it's always traceable to
     * exactly the run that last (re)calculated it.
     */
    @Transactional
    public BigDecimal computeMonthlyRecovery(Long tenantId, Long employeeId, int year, int month, Long payrollRunId, BigDecimal maxAllowed) {
        BigDecimal remainingCapacity = maxAllowed.max(BigDecimal.ZERO);
        BigDecimal totalRecovered = BigDecimal.ZERO;
        List<EmployeeAdvance> activeAdvances = advanceRepository
                .findAllByClientCompanyIdAndEmployeeIdAndStatusOrderByAdvanceDateAsc(tenantId, employeeId, "ACTIVE");

        for (EmployeeAdvance advance : activeAdvances) {
            if (remainingCapacity.signum() <= 0) break;
            // Recovery Start Month (spec section 9/10): skip an advance entirely until the
            // payroll month being processed reaches its configured start month - e.g. an advance
            // granted in August with "Recovery Start: September 2026" contributes nothing to
            // August's payroll.
            if (year < advance.getRecoveryStartYear()
                    || (year == advance.getRecoveryStartYear() && month < advance.getRecoveryStartMonth())) {
                continue;
            }
            // Pause/resume: admin can turn payroll recovery off for this advance temporarily
            // (e.g. it was already paid manually this month via Settle Partial) without touching
            // status or monthlyRecoveryAmount - see EmployeeAdvance.recoverViaPayroll.
            if (!advance.isRecoverViaPayroll()) {
                continue;
            }
            BigDecimal outstanding = getOutstanding(advance);
            if (outstanding.signum() <= 0) continue;

            BigDecimal recovery = advance.getMonthlyRecoveryAmount().min(outstanding).min(remainingCapacity);
            if (recovery.signum() <= 0) continue;

            AdvanceRecoveryTransaction txn = recoveryRepository.findByAdvanceIdAndYearAndMonthAndSource(advance.getId(), year, month, "PAYROLL")
                    .orElseGet(() -> {
                        AdvanceRecoveryTransaction created = new AdvanceRecoveryTransaction();
                        created.setClientCompanyId(tenantId);
                        created.setEmployeeId(employeeId);
                        created.setAdvanceId(advance.getId());
                        created.setYear(year);
                        created.setMonth(month);
                        created.setSource("PAYROLL");
                        return created;
                    });
            txn.setRecoveredAmount(recovery);
            txn.setPayrollRunId(payrollRunId);
            recoveryRepository.save(txn);

            remainingCapacity = remainingCapacity.subtract(recovery);
            totalRecovered = totalRecovered.add(recovery);
        }
        return totalRecovered;
    }

    private BigDecimal getOutstanding(EmployeeAdvance advance) {
        if ("SETTLED".equals(advance.getStatus()) || "CANCELLED".equals(advance.getStatus())) {
            return BigDecimal.ZERO;
        }
        return outstandingFrom(advance, recoveryRepository.findAllByAdvanceId(advance.getId()));
    }

    /** Same computation as getOutstanding(), but takes an already-fetched transaction list instead of querying - lets bulk callers (getDashboardSummary(), getOutstandingForEmployee()) batch-fetch once for many advances instead of querying per advance. */
    private BigDecimal outstandingFrom(EmployeeAdvance advance, List<AdvanceRecoveryTransaction> transactions) {
        if ("SETTLED".equals(advance.getStatus()) || "CANCELLED".equals(advance.getStatus())) {
            return BigDecimal.ZERO;
        }
        BigDecimal recovered = transactions.stream()
                .map(AdvanceRecoveryTransaction::getRecoveredAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return advance.getAmount().subtract(recovered).max(BigDecimal.ZERO);
    }

    private EmployeeAdvance getAdvanceForEmployee(Long tenantId, Long employeeId, Long advanceId) {
        EmployeeAdvance advance = advanceRepository.findByIdAndClientCompanyId(advanceId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Advance " + advanceId + " does not belong to the current tenant"));
        if (!advance.getEmployeeId().equals(employeeId)) {
            throw new TenantAccessDeniedException("Advance " + advanceId + " does not belong to employee " + employeeId);
        }
        return advance;
    }

    private Employee validateEmployee(Long tenantId, Long employeeId) {
        return employeeRepository.findByIdAndClientCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Employee " + employeeId + " does not belong to the current tenant"));
    }

    private EmployeeAdvanceResponse toResponse(EmployeeAdvance a) {
        EmployeeAdvanceResponse r = new EmployeeAdvanceResponse();
        r.setId(a.getId());
        r.setEmployeeId(a.getEmployeeId());
        employeeRepository.findById(a.getEmployeeId()).ifPresent(emp -> {
            r.setEmployeeCode(emp.getEmployeeCode());
            r.setEmployeeName((emp.getFirstName() + " " + emp.getLastName()).trim());
        });
        r.setAdvanceDate(a.getAdvanceDate());
        r.setAmount(a.getAmount());
        r.setReason(a.getReason());
        r.setPaymentMode(a.getPaymentMode());
        r.setMonthlyRecoveryAmount(a.getMonthlyRecoveryAmount());
        r.setRecoveryStartYear(a.getRecoveryStartYear());
        r.setRecoveryStartMonth(a.getRecoveryStartMonth());
        r.setRemarks(a.getRemarks());
        r.setRecoverViaPayroll(a.isRecoverViaPayroll());
        r.setStatus(a.getStatus());
        List<AdvanceRecoveryTransaction> transactions = recoveryRepository.findAllByAdvanceId(a.getId());
        BigDecimal recovered = transactions.stream()
                .map(AdvanceRecoveryTransaction::getRecoveredAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        r.setRecoveredAmount(recovered);
        r.setOutstandingAmount(getOutstanding(a));
        // Installments actually paid via payroll so far (spec: "how many installments paid").
        // A MANUAL_SETTLEMENT payment isn't a payroll "installment" in the loan sense, so it's
        // deliberately not counted here - it still reduces outstandingAmount either way.
        r.setInstallmentsPaid((int) transactions.stream().filter(t -> "PAYROLL".equals(t.getSource())).count());
        r.setCreatedAt(a.getCreatedAt());
        r.setCreatedBy(a.getCreatedBy() == null ? "-" : userRepository.findById(a.getCreatedBy()).map(u -> u.getUsername()).orElse("-"));
        return r;
    }

    private AdvanceRecoveryTransactionResponse toRecoveryResponse(AdvanceRecoveryTransaction t) {
        AdvanceRecoveryTransactionResponse r = new AdvanceRecoveryTransactionResponse();
        r.setId(t.getId());
        r.setYear(t.getYear());
        r.setMonth(t.getMonth());
        r.setRecoveredAmount(t.getRecoveredAmount());
        r.setSource(t.getSource());
        r.setPayrollRunId(t.getPayrollRunId());
        r.setCreatedAt(t.getCreatedAt());
        r.setCreatedBy(t.getCreatedBy() == null ? "-" : userRepository.findById(t.getCreatedBy()).map(u -> u.getUsername()).orElse("-"));
        return r;
    }
}
