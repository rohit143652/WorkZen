package com.example.application.advance_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row per (advance, year, month) actually recovered - this is the
 * permanent record spec section 15 requires: "the new [recovery] amount
 * should apply from the selected effective month; historical recovery
 * records must not change." Regenerating the same month via payroll
 * calculation is idempotent (upserts this one row, keyed by advance+year+
 * month) but never touches other months' rows.
 *
 * source distinguishes how this row came to exist (architecture refactor
 * Phase 5, spec section 10/17):
 *   PAYROLL - created by PayrollCalculationService during a Payroll Run
 *     calculation; payrollRunId identifies exactly which run, answering
 *     "which payroll recovered this amount?"
 *   MANUAL_SETTLEMENT - the employee paid some or all of the outstanding
 *     amount outside payroll (see EmployeeAdvanceService.settlePartial());
 *     payrollRunId is null since no payroll run is involved. This reuses
 *     the same recoveredAmount-summing mechanism getOutstanding() already
 *     uses, so a partial manual settlement correctly reduces outstanding
 *     without a second, parallel bookkeeping structure.
 */
@Entity
@Table(name = "advance_recovery_transactions")
public class AdvanceRecoveryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "advance_id", nullable = false)
    private Long advanceId;

    /** Null for MANUAL_SETTLEMENT rows - see the source field. */
    @Column(name = "payroll_run_id")
    private Long payrollRunId;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(name = "recovered_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal recoveredAmount;

    /** PAYROLL or MANUAL_SETTLEMENT. */
    @Column(nullable = false, length = 20)
    private String source = "PAYROLL";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Long getAdvanceId() { return advanceId; }
    public void setAdvanceId(Long advanceId) { this.advanceId = advanceId; }
    public Long getPayrollRunId() { return payrollRunId; }
    public void setPayrollRunId(Long payrollRunId) { this.payrollRunId = payrollRunId; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public BigDecimal getRecoveredAmount() { return recoveredAmount; }
    public void setRecoveredAmount(BigDecimal recoveredAmount) { this.recoveredAmount = recoveredAmount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
