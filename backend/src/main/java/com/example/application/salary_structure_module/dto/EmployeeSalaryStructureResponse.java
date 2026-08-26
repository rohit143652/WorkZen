package com.example.application.salary_structure_module.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeSalaryStructureResponse {
    private Long id;
    private Long salaryStructureId;
    private String structureCode;
    private String structureName;
    private String salaryType;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
    /**
     * Computed from the structure's CURRENT definition at read time, not a
     * frozen snapshot - if the structure's components change later, this
     * reflects the new values immediately. Architecture refactor Phase 3:
     * this is Gross Earnings ONLY - PF/ESI/PT/Tax/Advance Recovery/Net Pay
     * are never calculated here. True point-in-time historical accuracy for
     * a specific past payroll period comes from PayrollRunEmployee, which
     * snapshots these values permanently at calculation time - see
     * payroll_module.PayrollRunService.
     */
    private BigDecimal grossEarnings;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSalaryStructureId() { return salaryStructureId; }
    public void setSalaryStructureId(Long salaryStructureId) { this.salaryStructureId = salaryStructureId; }
    public String getStructureCode() { return structureCode; }
    public void setStructureCode(String structureCode) { this.structureCode = structureCode; }
    public String getStructureName() { return structureName; }
    public void setStructureName(String structureName) { this.structureName = structureName; }
    public String getSalaryType() { return salaryType; }
    public void setSalaryType(String salaryType) { this.salaryType = salaryType; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getGrossEarnings() { return grossEarnings; }
    public void setGrossEarnings(BigDecimal grossEarnings) { this.grossEarnings = grossEarnings; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
