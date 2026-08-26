package com.example.application.advance_module.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One recovery event - either a PAYROLL-driven deduction (traceable to exactly one Payroll Run) or a MANUAL_SETTLEMENT paid outside payroll. */
public class AdvanceRecoveryTransactionResponse {
    private Long id;
    private int year;
    private int month;
    private BigDecimal recoveredAmount;
    private String source;
    /** Null for MANUAL_SETTLEMENT rows. */
    private Long payrollRunId;
    private LocalDateTime createdAt;
    private String createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public BigDecimal getRecoveredAmount() { return recoveredAmount; }
    public void setRecoveredAmount(BigDecimal recoveredAmount) { this.recoveredAmount = recoveredAmount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getPayrollRunId() { return payrollRunId; }
    public void setPayrollRunId(Long payrollRunId) { this.payrollRunId = payrollRunId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
