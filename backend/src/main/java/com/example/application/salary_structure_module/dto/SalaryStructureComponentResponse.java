package com.example.application.salary_structure_module.dto;

import java.math.BigDecimal;

public class SalaryStructureComponentResponse {
    private Long id;
    private Long salaryComponentId;
    private String componentCode;
    private String componentName;
    private String componentType;
    private String calculationType;
    private BigDecimal amount;
    private BigDecimal percentage;
    /** The actual computed amount for this component within this structure - e.g. 10% of basic already resolved to a rupee figure. */
    private BigDecimal resolvedAmount;
    private int displayOrder;

    public SalaryStructureComponentResponse(Long id, Long salaryComponentId, String componentCode, String componentName,
                                             String componentType, String calculationType, BigDecimal amount,
                                             BigDecimal percentage, BigDecimal resolvedAmount, int displayOrder) {
        this.id = id;
        this.salaryComponentId = salaryComponentId;
        this.componentCode = componentCode;
        this.componentName = componentName;
        this.componentType = componentType;
        this.calculationType = calculationType;
        this.amount = amount;
        this.percentage = percentage;
        this.resolvedAmount = resolvedAmount;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public Long getSalaryComponentId() { return salaryComponentId; }
    public String getComponentCode() { return componentCode; }
    public String getComponentName() { return componentName; }
    public String getComponentType() { return componentType; }
    public String getCalculationType() { return calculationType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPercentage() { return percentage; }
    public BigDecimal getResolvedAmount() { return resolvedAmount; }
    public int getDisplayOrder() { return displayOrder; }
}
