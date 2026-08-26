package com.example.application.salary_structure_module.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * One line item within a Salary Structure, linking to a SalaryComponent
 * but with its OWN calculationType/amount/percentage - so the same "HRA"
 * component can be a fixed 3000 in one structure and 10% of basic in
 * another. Plain Long FKs (not JPA relations) to match this codebase's
 * established style of resolving cross-entity references via repository
 * lookups in the service layer rather than entity graphs.
 */
@Entity
@Table(name = "salary_structure_components")
public class SalaryStructureComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "salary_structure_id", nullable = false)
    private Long salaryStructureId;

    @Column(name = "salary_component_id", nullable = false)
    private Long salaryComponentId;

    @Column(name = "calculation_type", nullable = false, length = 30)
    private String calculationType;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSalaryStructureId() { return salaryStructureId; }
    public void setSalaryStructureId(Long salaryStructureId) { this.salaryStructureId = salaryStructureId; }
    public Long getSalaryComponentId() { return salaryComponentId; }
    public void setSalaryComponentId(Long salaryComponentId) { this.salaryComponentId = salaryComponentId; }
    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
