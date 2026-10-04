package com.example.application.payroll_module.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProfessionalTaxSlabRequest {
    /** Null = applies regardless of state. */
    private String state;
    private BigDecimal minSalary;
    /** Null = unbounded (the top slab). */
    private BigDecimal maxSalary;
    private BigDecimal ptAmount;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }
    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }
    public BigDecimal getPtAmount() { return ptAmount; }
    public void setPtAmount(BigDecimal ptAmount) { this.ptAmount = ptAmount; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
}
