package com.example.application.leave_module.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class ExtraPaidLeaveRequest {
    @NotNull(message = "leaveDays is required")
    @DecimalMin(value = "0.01", message = "leaveDays must be greater than 0")
    private java.math.BigDecimal leaveDays;

    /** MEDICAL, SPECIAL, EMERGENCY, OTHER */
    @NotBlank(message = "reason is required")
    private String reason;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private String remark;

    public java.math.BigDecimal getLeaveDays() { return leaveDays; }
    public void setLeaveDays(java.math.BigDecimal leaveDays) { this.leaveDays = leaveDays; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
