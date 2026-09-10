package com.example.application.subscription_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The CURRENT subscription for one client company - one row per company (see the unique
 * constraint on client_company_id), always reflecting whatever plan/cycle/status is active
 * right now. Changing any of this does not create a new row here; it updates this one row and
 * appends a SubscriptionHistory entry describing the change - this table is "now", history is
 * "how we got here".
 *
 * employeeLimitOverride/monthlyPriceOverride/yearlyPriceOverride exist for two reasons: (1) an
 * Enterprise plan's own limit/price fields are null by design (see SubscriptionPlan javadoc), so
 * THIS is where that client's actual negotiated number lives; (2) any other plan's default can
 * still be overridden per-client for a one-off arrangement without needing a whole new plan
 * definition just for one client.
 */
@Entity
@Table(name = "client_subscriptions")
public class ClientSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false, unique = true)
    private Long clientCompanyId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    /** MONTHLY, YEARLY, or CUSTOM. */
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private String billingCycle = "MONTHLY";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** TRIAL, ACTIVE, EXPIRING_SOON, EXPIRED, GRACE_PERIOD, SUSPENDED, CANCELLED - see SubscriptionAccessPolicy for what each allows. */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "employee_limit_override")
    private Integer employeeLimitOverride;

    @Column(name = "monthly_price_override", precision = 10, scale = 2)
    private BigDecimal monthlyPriceOverride;

    @Column(name = "yearly_price_override", precision = 10, scale = 2)
    private BigDecimal yearlyPriceOverride;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getEmployeeLimitOverride() { return employeeLimitOverride; }
    public void setEmployeeLimitOverride(Integer employeeLimitOverride) { this.employeeLimitOverride = employeeLimitOverride; }
    public BigDecimal getMonthlyPriceOverride() { return monthlyPriceOverride; }
    public void setMonthlyPriceOverride(BigDecimal monthlyPriceOverride) { this.monthlyPriceOverride = monthlyPriceOverride; }
    public BigDecimal getYearlyPriceOverride() { return yearlyPriceOverride; }
    public void setYearlyPriceOverride(BigDecimal yearlyPriceOverride) { this.yearlyPriceOverride = yearlyPriceOverride; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
