package com.example.application.salary_structure_module.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SalaryStructureRequest {
    /** Optional. If left blank, the backend auto-generates the next sequential code for this tenant (e.g. SS0001, SS0002...). */
    private String structureCode;

    @NotBlank(message = "Structure name is required")
    private String structureName;

    /** MONTHLY, DAILY, HOURLY, CONTRACT */
    @NotBlank(message = "Salary type is required")
    private String salaryType = "MONTHLY";

    private String description;

    @DecimalMin(value = "0", message = "Daily rate must be >= 0")
    private BigDecimal dailyRate;

    @DecimalMin(value = "0", message = "Hourly rate must be >= 0")
    private BigDecimal hourlyRate;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @NotEmpty(message = "At least one salary component is required")
    @Valid
    private List<SalaryStructureComponentRequest> components;

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
    public List<SalaryStructureComponentRequest> getComponents() { return components; }
    public void setComponents(List<SalaryStructureComponentRequest> components) { this.components = components; }
}
