package com.example.application.subscription_module.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SubscriptionPlanResponse {
    private Long id;
    private String planCode;
    private String planName;
    private String description;
    private Integer employeeLimit;
    private boolean customEmployeeLimitAllowed;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private boolean active;
    private int displayOrder;
    private List<String> featureCodes;
    /** How many client companies currently subscribe to this plan - drives the "don't delete a plan in use" rule on the frontend/service. */
    private long clientCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getEmployeeLimit() { return employeeLimit; }
    public void setEmployeeLimit(Integer employeeLimit) { this.employeeLimit = employeeLimit; }
    public boolean isCustomEmployeeLimitAllowed() { return customEmployeeLimitAllowed; }
    public void setCustomEmployeeLimitAllowed(boolean customEmployeeLimitAllowed) { this.customEmployeeLimitAllowed = customEmployeeLimitAllowed; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public BigDecimal getYearlyPrice() { return yearlyPrice; }
    public void setYearlyPrice(BigDecimal yearlyPrice) { this.yearlyPrice = yearlyPrice; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public List<String> getFeatureCodes() { return featureCodes; }
    public void setFeatureCodes(List<String> featureCodes) { this.featureCodes = featureCodes; }
    public long getClientCount() { return clientCount; }
    public void setClientCount(long clientCount) { this.clientCount = clientCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
