package com.example.application.salary_structure_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SalaryStructureComponentRequest {
    @NotNull(message = "Salary component is required")
    private Long salaryComponentId;

    @NotBlank(message = "Calculation type is required")
    private String calculationType;

    private BigDecimal amount;
    private BigDecimal percentage;
    private int displayOrder = 0;

    public Long getSalaryComponentId() { return salaryComponentId; }
    public void setSalaryComponentId(Long salaryComponentId) { this.salaryComponentId = salaryComponentId; }
    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
