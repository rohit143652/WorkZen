package com.example.application.payroll_module.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OvertimeRecordRequest {
    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotNull(message = "overtimeDate is required")
    @PastOrPresent(message = "overtimeDate cannot be in the future")
    private LocalDate overtimeDate;

    @NotNull(message = "hours is required")
    @DecimalMin(value = "0.25", message = "hours must be at least 0.25")
    @DecimalMax(value = "24", message = "hours cannot exceed 24 for a single day")
    private BigDecimal hours;

    /** The actual rupee amount to pay for this day's overtime - entered directly, not derived
        from a fixed rate (see EmployeeOvertimeRecord javadoc). */
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0", message = "amount must be >= 0")
    private BigDecimal amount;

    private String remarks;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getOvertimeDate() { return overtimeDate; }
    public void setOvertimeDate(LocalDate overtimeDate) { this.overtimeDate = overtimeDate; }
    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
