package com.example.application.subscription_module.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ClientSubscriptionResponse {
    private Long id;
    private Long clientCompanyId;
    private Long planId;
    private String planCode;
    private String planName;
    private String billingCycle;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long daysRemaining;
    private String status;
    private Integer employeeLimitOverride;
    /** planId's employeeLimit unless employeeLimitOverride is set - what actually applies. */
    private Integer effectiveEmployeeLimit;
    private BigDecimal monthlyPriceOverride;
    private BigDecimal effectiveMonthlyPrice;
    private BigDecimal yearlyPriceOverride;
    private BigDecimal effectiveYearlyPrice;
    private long activeEmployeeCount;
    private String notes;
    private List<String> enabledFeatures;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Long daysRemaining) { this.daysRemaining = daysRemaining; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getEmployeeLimitOverride() { return employeeLimitOverride; }
    public void setEmployeeLimitOverride(Integer employeeLimitOverride) { this.employeeLimitOverride = employeeLimitOverride; }
    public Integer getEffectiveEmployeeLimit() { return effectiveEmployeeLimit; }
    public void setEffectiveEmployeeLimit(Integer effectiveEmployeeLimit) { this.effectiveEmployeeLimit = effectiveEmployeeLimit; }
    public BigDecimal getMonthlyPriceOverride() { return monthlyPriceOverride; }
    public void setMonthlyPriceOverride(BigDecimal monthlyPriceOverride) { this.monthlyPriceOverride = monthlyPriceOverride; }
    public BigDecimal getEffectiveMonthlyPrice() { return effectiveMonthlyPrice; }
    public void setEffectiveMonthlyPrice(BigDecimal effectiveMonthlyPrice) { this.effectiveMonthlyPrice = effectiveMonthlyPrice; }
    public BigDecimal getYearlyPriceOverride() { return yearlyPriceOverride; }
    public void setYearlyPriceOverride(BigDecimal yearlyPriceOverride) { this.yearlyPriceOverride = yearlyPriceOverride; }
    public BigDecimal getEffectiveYearlyPrice() { return effectiveYearlyPrice; }
    public void setEffectiveYearlyPrice(BigDecimal effectiveYearlyPrice) { this.effectiveYearlyPrice = effectiveYearlyPrice; }
    public long getActiveEmployeeCount() { return activeEmployeeCount; }
    public void setActiveEmployeeCount(long activeEmployeeCount) { this.activeEmployeeCount = activeEmployeeCount; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<String> getEnabledFeatures() { return enabledFeatures; }
    public void setEnabledFeatures(List<String> enabledFeatures) { this.enabledFeatures = enabledFeatures; }
}
