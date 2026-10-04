package com.example.application.payroll_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One slab of a company's Professional Tax structure (spec section 12) - a given gross salary
 * falls into exactly one slab (minSalary <= gross, and maxSalary is either null or >= gross),
 * each slab specifying the flat PT amount that applies. Effective-dated the same way
 * PayrollSettings is: editing the slab table never mutates an already-effective row, it only
 * ever closes it out (effectiveTo) and creates new rows - so an already-approved month's PT
 * stays reproducible even if the slab structure changes later.
 *
 * Only consulted when the owning PayrollSettings.ptCalculationMode is SLAB - under FLAT (the
 * default), PayrollSettings.professionalTax is used directly and this table is never queried.
 */
@Entity
@Table(name = "professional_tax_slabs")
public class ProfessionalTaxSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    /** Null = applies regardless of state - most companies operate in a single state and never need this distinction. */
    @Column(length = 100)
    private String state;

    @Column(name = "min_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal minSalary;

    /** Null = unbounded (the top slab - any gross salary at or above minSalary with no other matching slab). */
    @Column(name = "max_salary", precision = 12, scale = 2)
    private BigDecimal maxSalary;

    @Column(name = "pt_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal ptAmount;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Null = open-ended (still the latest slab table for this range). */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /** ACTIVE or CANCELLED - a cancelled future (not-yet-effective) slab is excluded from resolution entirely. */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
