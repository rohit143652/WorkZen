package com.example.application.salary_structure_module.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AssignSalaryStructureRequest {
    @NotNull(message = "Salary structure is required")
    private Long salaryStructureId;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    public Long getSalaryStructureId() { return salaryStructureId; }
    public void setSalaryStructureId(Long salaryStructureId) { this.salaryStructureId = salaryStructureId; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
