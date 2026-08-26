package com.example.application.payroll_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One EFFECTIVE-DATED payroll configuration record for one tenant
 * (architecture refactor Phase 8) - a tenant can have any number of these
 * over time, each covering a date range via effectiveFrom/effectiveTo.
 * Was a single mutable row per tenant (PK = clientCompanyId); refactored
 * in place into this history-capable shape rather than building a
 * separate PayrollSettingsHistory table, per the explicit "do not create
 * both systems unnecessarily" instruction.
 *
 * Never read directly by PayrollCalculationService/PayrollRunService -
 * always resolved via PayrollSettingsResolver.resolve(tenantId, year,
 * month), which picks whichever ACTIVE row's window actually covers that
 * payroll month. Editing "today's" settings only ever creates a NEW row
 * effective from today/a future date (see PayrollSettingsService); it
 * never mutates a past row's percentages, so an already-approved month's
 * applicable configuration can never change out from under it.
 */
@Entity
@Table(name = "payroll_settings")
public class PayrollSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    /** The first calendar day this configuration applies to. */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Null = open-ended (still the latest configuration). Set automatically when a newer configuration is created - see PayrollSettingsService.createFutureConfig(). */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /** ACTIVE or CANCELLED - a cancelled future (not-yet-effective) configuration is excluded from resolution entirely. */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "epf_enabled", nullable = false)
    private boolean epfEnabled = true;

    @Column(name = "epf_employee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal epfEmployeePercent = new BigDecimal("12.00");

    @Column(name = "epf_employer_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal epfEmployerPercent = new BigDecimal("13.00");

    @Column(name = "esi_enabled", nullable = false)
    private boolean esiEnabled = true;

    @Column(name = "esi_employee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal esiEmployeePercent = new BigDecimal("0.75");

    @Column(name = "esi_employer_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal esiEmployerPercent = new BigDecimal("3.25");

    /** Employees whose Total Gross exceeds this are not charged ESI. Null = no ceiling (always applies when esiEnabled). */
    @Column(name = "esi_wage_ceiling", precision = 12, scale = 2)
    private BigDecimal esiWageCeiling = new BigDecimal("21000.00");

    @Column(name = "pt_enabled", nullable = false)
    private boolean ptEnabled = true;

    /** Flat monthly amount. A slab-based table is a reasonable future enhancement, not implemented here - no existing state-specific PT slab structure was found in the project to reuse (spec section 11). */
    @Column(name = "professional_tax", nullable = false, precision = 10, scale = 2)
    private BigDecimal professionalTax = new BigDecimal("200.00");

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isEpfEnabled() { return epfEnabled; }
    public void setEpfEnabled(boolean epfEnabled) { this.epfEnabled = epfEnabled; }
    public BigDecimal getEpfEmployeePercent() { return epfEmployeePercent; }
    public void setEpfEmployeePercent(BigDecimal epfEmployeePercent) { this.epfEmployeePercent = epfEmployeePercent; }
    public BigDecimal getEpfEmployerPercent() { return epfEmployerPercent; }
    public void setEpfEmployerPercent(BigDecimal epfEmployerPercent) { this.epfEmployerPercent = epfEmployerPercent; }
    public boolean isEsiEnabled() { return esiEnabled; }
    public void setEsiEnabled(boolean esiEnabled) { this.esiEnabled = esiEnabled; }
    public BigDecimal getEsiEmployeePercent() { return esiEmployeePercent; }
    public void setEsiEmployeePercent(BigDecimal esiEmployeePercent) { this.esiEmployeePercent = esiEmployeePercent; }
    public BigDecimal getEsiEmployerPercent() { return esiEmployerPercent; }
    public void setEsiEmployerPercent(BigDecimal esiEmployerPercent) { this.esiEmployerPercent = esiEmployerPercent; }
    public BigDecimal getEsiWageCeiling() { return esiWageCeiling; }
    public void setEsiWageCeiling(BigDecimal esiWageCeiling) { this.esiWageCeiling = esiWageCeiling; }
    public boolean isPtEnabled() { return ptEnabled; }
    public void setPtEnabled(boolean ptEnabled) { this.ptEnabled = ptEnabled; }
    public BigDecimal getProfessionalTax() { return professionalTax; }
    public void setProfessionalTax(BigDecimal professionalTax) { this.professionalTax = professionalTax; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
