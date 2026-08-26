package com.example.application.salary_structure_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A named, reusable payroll template built from Salary Components (see
 * SalaryStructureComponent for the line items). Employees are linked to a
 * structure via EmployeeSalaryStructure, which preserves history rather
 * than pointing directly at this row, so a structure's own effective_from/
 * effective_to describe when the TEMPLATE itself is valid to assign, not
 * any individual employee's assignment period.
 */
@Entity
@Table(name = "salary_structures")
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "structure_code", nullable = false, length = 50)
    private String structureCode;

    @Column(name = "structure_name", nullable = false, length = 150)
    private String structureName;

    /** MONTHLY, DAILY, HOURLY, CONTRACT (spec section 13). Drives which of dailyRate/hourlyRate/components is authoritative. */
    @Column(name = "salary_type", nullable = false, length = 20)
    private String salaryType = "MONTHLY";

    @Column(length = 255)
    private String description;

    /** Reference rate for DAILY structures. Not multiplied by attendance here - that's future Payroll's job. */
    @Column(name = "daily_rate", precision = 12, scale = 2)
    private BigDecimal dailyRate;

    /** Reference rate for HOURLY structures. Not multiplied by attendance here - that's future Payroll's job. */
    @Column(name = "hourly_rate", precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

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
    public String getStructureCode() { return structureCode; }
    public void setStructureCode(String structureCode) { this.structureCode = structureCode; }
    public String getStructureName() { return structureName; }
    public void setStructureName(String structureName) { this.structureName = structureName; }
    public String getSalaryType() { return salaryType; }
    public void setSalaryType(String salaryType) { this.salaryType = salaryType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
