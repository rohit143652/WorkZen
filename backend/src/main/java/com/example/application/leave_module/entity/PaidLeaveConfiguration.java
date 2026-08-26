package com.example.application.leave_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One EFFECTIVE-DATED leave policy record for one tenant (architecture
 * refactor Phase 9) - a tenant can have any number of these over time,
 * each covering a date range via effectiveFrom/effectiveTo. Was a single
 * mutable row per tenant (PK = clientCompanyId); refactored in place into
 * this history-capable shape, mirroring PayrollSettings (Phase 8), rather
 * than building a separate LeavePolicyHistory table.
 *
 * Never read directly by EmployeePaidLeaveService - always resolved via
 * LeavePolicyResolver.resolve(tenantId, year, month), which picks
 * whichever ACTIVE row's window actually covers that leave month. Editing
 * "today's" policy only ever creates a NEW row effective from today/a
 * future date (see PaidLeaveConfigService); it never mutates a past row's
 * settings, so an already-generated month's applicable policy can never
 * change out from under it (spec section 16).
 */
@Entity
@Table(name = "paid_leave_configurations")
public class PaidLeaveConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    /** The first calendar day this policy applies to. */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Null = open-ended (still the latest policy). Set automatically when a newer policy is created - see PaidLeaveConfigService.createFutureConfig(). */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /** ACTIVE or CANCELLED - a cancelled future (not-yet-effective) policy is excluded from resolution entirely. */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "monthly_paid_leave", nullable = false)
    private int monthlyPaidLeave = 2;

    /** Master switch (spec: client decides whether Paid Leave is active or fully off). False = no new monthly entitlement accrues while this policy is in effect - see LeavePolicyResolver. */
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "allow_carry_forward", nullable = false)
    private boolean allowCarryForward = true;

    /** Null = unlimited carry-forward. */
    @Column(name = "maximum_carry_forward")
    private Integer maximumCarryForward;

    /**
     * True = carry-forward only within the same calendar year - balance
     * automatically resets to 0 at the January boundary (e.g. December
     * 2026's leftover balance does NOT carry into January 2027). False
     * (default) = carry-forward continues indefinitely across years.
     */
    @Column(name = "reset_annually", nullable = false)
    private boolean resetAnnually = false;

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
    public int getMonthlyPaidLeave() { return monthlyPaidLeave; }
    public void setMonthlyPaidLeave(int monthlyPaidLeave) { this.monthlyPaidLeave = monthlyPaidLeave; }
    /** What actually accrues this month - 0 whenever the master switch is off, regardless of the configured monthlyPaidLeave number. Use this (not getMonthlyPaidLeave()) wherever entitlement is actually calculated. */
    public int getEffectiveMonthlyPaidLeave() { return enabled ? monthlyPaidLeave : 0; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isAllowCarryForward() { return allowCarryForward; }
    public void setAllowCarryForward(boolean allowCarryForward) { this.allowCarryForward = allowCarryForward; }
    public Integer getMaximumCarryForward() { return maximumCarryForward; }
    public void setMaximumCarryForward(Integer maximumCarryForward) { this.maximumCarryForward = maximumCarryForward; }
    public boolean isResetAnnually() { return resetAnnually; }
    public void setResetAnnually(boolean resetAnnually) { this.resetAnnually = resetAnnually; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
