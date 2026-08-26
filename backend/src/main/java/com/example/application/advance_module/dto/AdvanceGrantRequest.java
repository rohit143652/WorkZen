package com.example.application.advance_module.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdvanceGrantRequest {
    @NotNull(message = "advanceDate is required")
    private LocalDate advanceDate;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    private String reason;

    /** CASH, BANK_TRANSFER, OTHER */
    private String paymentMode;

    @NotNull(message = "monthlyRecoveryAmount is required")
    @DecimalMin(value = "0", message = "monthlyRecoveryAmount must be >= 0")
    private BigDecimal monthlyRecoveryAmount;

    /** Optional - the first payroll month recovery may begin. Null means "same month as advanceDate" (the previous, still-supported default behavior). */
    private Integer recoveryStartYear;
    private Integer recoveryStartMonth;

    private String remarks;

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
    public Integer getRecoveryStartYear() { return recoveryStartYear; }
    public void setRecoveryStartYear(Integer recoveryStartYear) { this.recoveryStartYear = recoveryStartYear; }
    public Integer getRecoveryStartMonth() { return recoveryStartMonth; }
    public void setRecoveryStartMonth(Integer recoveryStartMonth) { this.recoveryStartMonth = recoveryStartMonth; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
