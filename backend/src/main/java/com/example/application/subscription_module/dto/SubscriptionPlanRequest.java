package com.example.application.subscription_module.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class SubscriptionPlanRequest {
    @NotBlank(message = "planCode is required")
    private String planCode;

    @NotBlank(message = "planName is required")
    private String planName;

    private String description;

    /** Required unless customEmployeeLimitAllowed is true - validated in the service, not here, since the rule spans two fields. */
    private Integer employeeLimit;

    @NotNull(message = "customEmployeeLimitAllowed is required")
    private Boolean customEmployeeLimitAllowed;

    @DecimalMin(value = "0", message = "monthlyPrice must be >= 0")
    private BigDecimal monthlyPrice;

    @DecimalMin(value = "0", message = "yearlyPrice must be >= 0")
    private BigDecimal yearlyPrice;

    private int displayOrder;

    /** Feature codes this plan includes by default - every code must be a valid FeatureCode constant, validated in the service against the centralized catalog. */
    private List<String> featureCodes = List.of();

    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getEmployeeLimit() { return employeeLimit; }
    public void setEmployeeLimit(Integer employeeLimit) { this.employeeLimit = employeeLimit; }
    public Boolean getCustomEmployeeLimitAllowed() { return customEmployeeLimitAllowed; }
    public void setCustomEmployeeLimitAllowed(Boolean customEmployeeLimitAllowed) { this.customEmployeeLimitAllowed = customEmployeeLimitAllowed; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public BigDecimal getYearlyPrice() { return yearlyPrice; }
    public void setYearlyPrice(BigDecimal yearlyPrice) { this.yearlyPrice = yearlyPrice; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public List<String> getFeatureCodes() { return featureCodes; }
    public void setFeatureCodes(List<String> featureCodes) { this.featureCodes = featureCodes; }
}
