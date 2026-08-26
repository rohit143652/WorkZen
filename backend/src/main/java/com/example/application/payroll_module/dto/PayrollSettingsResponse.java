package com.example.application.payroll_module.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PayrollSettingsResponse {
    private Long id;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
    private boolean epfEnabled;
    private BigDecimal epfEmployeePercent;
    private BigDecimal epfEmployerPercent;
    private boolean esiEnabled;
    private BigDecimal esiEmployeePercent;
    private BigDecimal esiEmployerPercent;
    private BigDecimal esiWageCeiling;
    private boolean ptEnabled;
    private BigDecimal professionalTax;

    public PayrollSettingsResponse(boolean epfEnabled, BigDecimal epfEmployeePercent, BigDecimal epfEmployerPercent,
                                    boolean esiEnabled, BigDecimal esiEmployeePercent, BigDecimal esiEmployerPercent,
                                    BigDecimal esiWageCeiling, boolean ptEnabled, BigDecimal professionalTax) {
        this.epfEnabled = epfEnabled;
        this.epfEmployeePercent = epfEmployeePercent;
        this.epfEmployerPercent = epfEmployerPercent;
        this.esiEnabled = esiEnabled;
        this.esiEmployeePercent = esiEmployeePercent;
        this.esiEmployerPercent = esiEmployerPercent;
        this.esiWageCeiling = esiWageCeiling;
        this.ptEnabled = ptEnabled;
        this.professionalTax = professionalTax;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isEpfEnabled() { return epfEnabled; }
    public BigDecimal getEpfEmployeePercent() { return epfEmployeePercent; }
    public BigDecimal getEpfEmployerPercent() { return epfEmployerPercent; }
    public boolean isEsiEnabled() { return esiEnabled; }
    public BigDecimal getEsiEmployeePercent() { return esiEmployeePercent; }
    public BigDecimal getEsiEmployerPercent() { return esiEmployerPercent; }
    public BigDecimal getEsiWageCeiling() { return esiWageCeiling; }
    public boolean isPtEnabled() { return ptEnabled; }
    public BigDecimal getProfessionalTax() { return professionalTax; }
}
