package com.example.application.payroll_module.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PayrollSettingsRequest {
    /** Required when scheduling a new configuration (POST); ignored when editing a not-yet-effective one (PUT), which keeps its original effective date. */
    private LocalDate effectiveFrom;

    @NotNull private Boolean epfEnabled;
    @NotNull @DecimalMin("0") private BigDecimal epfEmployeePercent;
    @NotNull @DecimalMin("0") private BigDecimal epfEmployerPercent;

    @NotNull private Boolean esiEnabled;
    @NotNull @DecimalMin("0") private BigDecimal esiEmployeePercent;
    @NotNull @DecimalMin("0") private BigDecimal esiEmployerPercent;
    /** Null = no ceiling. */
    @DecimalMin("0") private BigDecimal esiWageCeiling;

    @NotNull private Boolean ptEnabled;
    @NotNull @DecimalMin("0") private BigDecimal professionalTax;

    public Boolean getEpfEnabled() { return epfEnabled; }
    public void setEpfEnabled(Boolean epfEnabled) { this.epfEnabled = epfEnabled; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public BigDecimal getEpfEmployeePercent() { return epfEmployeePercent; }
    public void setEpfEmployeePercent(BigDecimal epfEmployeePercent) { this.epfEmployeePercent = epfEmployeePercent; }
    public BigDecimal getEpfEmployerPercent() { return epfEmployerPercent; }
    public void setEpfEmployerPercent(BigDecimal epfEmployerPercent) { this.epfEmployerPercent = epfEmployerPercent; }
    public Boolean getEsiEnabled() { return esiEnabled; }
    public void setEsiEnabled(Boolean esiEnabled) { this.esiEnabled = esiEnabled; }
    public BigDecimal getEsiEmployeePercent() { return esiEmployeePercent; }
    public void setEsiEmployeePercent(BigDecimal esiEmployeePercent) { this.esiEmployeePercent = esiEmployeePercent; }
    public BigDecimal getEsiEmployerPercent() { return esiEmployerPercent; }
    public void setEsiEmployerPercent(BigDecimal esiEmployerPercent) { this.esiEmployerPercent = esiEmployerPercent; }
    public BigDecimal getEsiWageCeiling() { return esiWageCeiling; }
    public void setEsiWageCeiling(BigDecimal esiWageCeiling) { this.esiWageCeiling = esiWageCeiling; }
    public Boolean getPtEnabled() { return ptEnabled; }
    public void setPtEnabled(Boolean ptEnabled) { this.ptEnabled = ptEnabled; }
    public BigDecimal getProfessionalTax() { return professionalTax; }
    public void setProfessionalTax(BigDecimal professionalTax) { this.professionalTax = professionalTax; }
}
