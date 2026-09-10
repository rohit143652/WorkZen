package com.example.application.subscription_module.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ClientSubscriptionRequest {
    @NotNull(message = "planId is required")
    private Long planId;

    @NotNull(message = "billingCycle is required")
    private String billingCycle;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "status is required")
    private String status;

    /** Required if the plan has customEmployeeLimitAllowed=true (Enterprise) and no fixed limit - validated in the service. */
    private Integer employeeLimitOverride;
    private BigDecimal monthlyPriceOverride;
    private BigDecimal yearlyPriceOverride;
    private String notes;

    /** Why this change is being made - stored on the SubscriptionHistory row, not the subscription itself. */
    private String reason;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getEmployeeLimitOverride() { return employeeLimitOverride; }
    public void setEmployeeLimitOverride(Integer employeeLimitOverride) { this.employeeLimitOverride = employeeLimitOverride; }
    public BigDecimal getMonthlyPriceOverride() { return monthlyPriceOverride; }
    public void setMonthlyPriceOverride(BigDecimal monthlyPriceOverride) { this.monthlyPriceOverride = monthlyPriceOverride; }
    public BigDecimal getYearlyPriceOverride() { return yearlyPriceOverride; }
    public void setYearlyPriceOverride(BigDecimal yearlyPriceOverride) { this.yearlyPriceOverride = yearlyPriceOverride; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
