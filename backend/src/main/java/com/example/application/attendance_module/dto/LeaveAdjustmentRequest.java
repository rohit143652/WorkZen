package com.example.application.attendance_module.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** paidDaysUsed = null clears the manual adjustment, reverting to the auto-calculated figure. */
public class LeaveAdjustmentRequest {
    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotNull(message = "year is required")
    private Integer year;

    @NotNull(message = "month is required")
    private Integer month;

    private BigDecimal paidDaysUsed;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public BigDecimal getPaidDaysUsed() { return paidDaysUsed; }
    public void setPaidDaysUsed(BigDecimal paidDaysUsed) { this.paidDaysUsed = paidDaysUsed; }
}
