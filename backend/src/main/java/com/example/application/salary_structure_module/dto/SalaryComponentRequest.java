package com.example.application.salary_structure_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SalaryComponentRequest {
    /** Optional. If left blank, the backend auto-generates the next sequential code for this tenant. */
    private String componentCode;

    @NotBlank(message = "Component name is required")
    private String componentName;

    @NotBlank(message = "Component type is required")
    private String componentType;

    @NotBlank(message = "Calculation type is required")
    private String calculationType;

    private BigDecimal value;
    private BigDecimal percentage;
    private boolean taxable = true;
    private int displayOrder = 0;

    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }
    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public boolean isTaxable() { return taxable; }
    public void setTaxable(boolean taxable) { this.taxable = taxable; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
