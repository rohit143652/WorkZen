package com.example.application.salary_structure_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A database-driven salary line item (BASIC, HRA, CONVEYANCE, PF, ESI...) -
 * never hardcoded. componentType says which side of the payslip it belongs
 * on; calculationType/value/percentage are this component's DEFAULTS, used
 * to pre-fill a new SalaryStructureComponent row - each structure can still
 * override calculationType/amount/percentage per its own
 * SalaryStructureComponent row. Deactivating never deletes: a component
 * already referenced by an existing structure must keep resolving.
 */
@Entity
@Table(name = "salary_components")
public class SalaryComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "component_code", nullable = false, length = 50)
    private String componentCode;

    @Column(name = "component_name", nullable = false, length = 150)
    private String componentName;

    /** EARNING, DEDUCTION, EMPLOYER_CONTRIBUTION, REIMBURSEMENT */
    @Column(name = "component_type", nullable = false, length = 30)
    private String componentType;

    /** FIXED, PERCENTAGE_OF_BASIC, PERCENTAGE_OF_GROSS, PER_DAY, PER_HOUR, MANUAL - see SalaryCalculationService for what's actually computed today. */
    @Column(name = "calculation_type", nullable = false, length = 30)
    private String calculationType;

    @Column(precision = 12, scale = 2)
    private BigDecimal value;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "is_taxable", nullable = false)
    private boolean taxable = true;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
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
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
