package com.example.application.salary_structure_module.dto;

import java.math.BigDecimal;

public class SalaryComponentResponse {
    private Long id;
    private String componentCode;
    private String componentName;
    private String componentType;
    private String calculationType;
    private BigDecimal value;
    private BigDecimal percentage;
    private boolean taxable;
    private boolean active;
    private int displayOrder;
    private long usageCount;

    public SalaryComponentResponse(Long id, String componentCode, String componentName, String componentType,
                                    String calculationType, BigDecimal value, BigDecimal percentage,
                                    boolean taxable, boolean active, int displayOrder, long usageCount) {
        this.id = id;
        this.componentCode = componentCode;
        this.componentName = componentName;
        this.componentType = componentType;
        this.calculationType = calculationType;
        this.value = value;
        this.percentage = percentage;
        this.taxable = taxable;
        this.active = active;
        this.displayOrder = displayOrder;
        this.usageCount = usageCount;
    }

    public Long getId() { return id; }
    public String getComponentCode() { return componentCode; }
    public String getComponentName() { return componentName; }
    public String getComponentType() { return componentType; }
    public String getCalculationType() { return calculationType; }
    public BigDecimal getValue() { return value; }
    public BigDecimal getPercentage() { return percentage; }
    public boolean isTaxable() { return taxable; }
    public boolean isActive() { return active; }
    public int getDisplayOrder() { return displayOrder; }
    public long getUsageCount() { return usageCount; }
}
