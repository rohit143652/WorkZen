package com.example.application.salary_structure_module.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SalaryStructureResponse {
    private Long id;
    private String structureCode;
    private String structureName;
    private String salaryType;
    private String description;
    private BigDecimal dailyRate;
    private BigDecimal hourlyRate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
    private List<SalaryStructureComponentResponse> components;
    /** Sum of this structure's EARNING/REIMBURSEMENT components - what the employee earns. Architecture refactor Phase 3: this is the ONLY financial figure Salary Structure is authoritative for - see PayrollCalculationService for PF/ESI/PT/Tax/Net Pay. */
    private BigDecimal grossEarnings;
    private long employeeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public List<SalaryStructureComponentResponse> getComponents() { return components; }
    public void setComponents(List<SalaryStructureComponentResponse> components) { this.components = components; }
    public BigDecimal getGrossEarnings() { return grossEarnings; }
    public void setGrossEarnings(BigDecimal grossEarnings) { this.grossEarnings = grossEarnings; }
    public long getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(long employeeCount) { this.employeeCount = employeeCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
