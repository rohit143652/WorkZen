package com.example.application.advance_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per advance an employee takes (spec section 12/13) - an employee
 * can have many of these, each tracked completely separately; none are
 * ever overwritten. Recovery against this specific advance is tracked in
 * AdvanceRecoveryTransaction (one row per month actually recovered), so
 * changing monthlyRecoveryAmount here only affects recovery computed
 * *going forward* - historical recovery transactions are never rewritten
 * (spec section 15).
 */
@Entity
@Table(name = "employee_advances")
public class EmployeeAdvance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "advance_date", nullable = false)
    private LocalDate advanceDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String reason;

    /** CASH, BANK_TRANSFER, OTHER */
    @Column(name = "payment_mode", length = 20)
    private String paymentMode;

    /** How much to recover per month once this advance starts being recovered - admin-editable at any time (spec section 15). */
    @Column(name = "monthly_recovery_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRecoveryAmount;

    /**
     * The first payroll month this advance is eligible for recovery - defaults to the advance
     * date's own month if not specified, so an advance normally starts being recovered the
     * same month it's granted. Set to a later month when the admin wants recovery to begin
     * only from a future payroll (e.g. "Recovery Start: September 2026" for an advance given in
     * August). computeMonthlyRecovery() skips any advance whose start month is still ahead of
     * the payroll month being processed - see EmployeeAdvanceService.
     */
    @Column(name = "recovery_start_year", nullable = false)
    private int recoveryStartYear;

    @Column(name = "recovery_start_month", nullable = false)
    private int recoveryStartMonth;

    /** Free-text notes, separate from reason (a short categorical-style label like "Personal Advance", "Medical Emergency"). */
    @Column(length = 500)
    private String remarks;

    /**
     * Pause/resume switch for PAYROLL-sourced recovery on this specific advance - independent of
     * monthlyRecoveryAmount and status. When false, computeMonthlyRecovery() skips this advance
     * entirely (e.g. the employee already paid this month's installment in cash via Settle
     * Partial, so payroll shouldn't also deduct it) - admin turns it back on for future months.
     * Manual settlement is never affected either way.
     */
    @Column(name = "recover_via_payroll", nullable = false)
    private boolean recoverViaPayroll = true;

    /** ACTIVE (still being recovered), SETTLED (paid off outside payroll - spec section 16), CANCELLED. */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    /** Set only when manually settled - the outstanding amount at the moment of settlement, so history shows what was written off via direct payment vs. payroll recovery. */
    @Column(name = "settled_amount", precision = 12, scale = 2)
    private BigDecimal settledAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getAdvanceDate() { return advanceDate; }
    public void setAdvanceDate(LocalDate advanceDate) { this.advanceDate = advanceDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    public BigDecimal getMonthlyRecoveryAmount() { return monthlyRecoveryAmount; }
    public void setMonthlyRecoveryAmount(BigDecimal monthlyRecoveryAmount) { this.monthlyRecoveryAmount = monthlyRecoveryAmount; }
    public int getRecoveryStartYear() { return recoveryStartYear; }
    public void setRecoveryStartYear(int recoveryStartYear) { this.recoveryStartYear = recoveryStartYear; }
    public int getRecoveryStartMonth() { return recoveryStartMonth; }
    public void setRecoveryStartMonth(int recoveryStartMonth) { this.recoveryStartMonth = recoveryStartMonth; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public boolean isRecoverViaPayroll() { return recoverViaPayroll; }
    public void setRecoverViaPayroll(boolean recoverViaPayroll) { this.recoverViaPayroll = recoverViaPayroll; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
