package com.example.application.payroll_module.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PayrollAdjustmentRequest {
    @NotNull(message = "otherManualDeduction is required")
    @DecimalMin(value = "0", message = "otherManualDeduction must be >= 0")
    private BigDecimal otherManualDeduction;

    @NotNull(message = "allowance is required")
    @DecimalMin(value = "0", message = "allowance must be >= 0")
    private BigDecimal allowance;

    public BigDecimal getOtherManualDeduction() { return otherManualDeduction; }
    public void setOtherManualDeduction(BigDecimal otherManualDeduction) { this.otherManualDeduction = otherManualDeduction; }
    public BigDecimal getAllowance() { return allowance; }
    public void setAllowance(BigDecimal allowance) { this.allowance = allowance; }
}
