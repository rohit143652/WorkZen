package com.example.application.advance_module.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AdvanceRecoveryAmountRequest {
    @NotNull(message = "monthlyRecoveryAmount is required")
    @DecimalMin(value = "0", message = "monthlyRecoveryAmount must be >= 0")
    private BigDecimal monthlyRecoveryAmount;

    public BigDecimal getMonthlyRecoveryAmount() { return monthlyRecoveryAmount; }
    public void setMonthlyRecoveryAmount(BigDecimal monthlyRecoveryAmount) { this.monthlyRecoveryAmount = monthlyRecoveryAmount; }
}
