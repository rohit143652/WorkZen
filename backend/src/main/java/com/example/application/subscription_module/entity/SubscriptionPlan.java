package com.example.application.subscription_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A subscription tier (Starter/Basic/Professional/Business/Enterprise) - defines the default
 * employee limit and pricing. What FEATURES a plan includes lives in PlanFeature, not here (kept
 * as a separate table so a plan's feature set can be edited independently of its price/limit,
 * and so FeatureAccessService can query "does this tenant's plan include X" without loading the
 * whole plan row).
 *
 * employeeLimit/monthlyPrice/yearlyPrice are nullable ONLY for a plan with
 * customEmployeeLimitAllowed=true (Enterprise) - "no fixed number, negotiated per client" is a
 * real, distinct state from "the number is zero", so NULL here specifically means "configured
 * per ClientSubscription instead", not "unlimited" and not "free". A ClientSubscription for such
 * a plan MUST set its own employeeLimitOverride/priceOverride - see ClientSubscriptionService.
 */
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_code", nullable = false, unique = true, length = 30)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(length = 500)
    private String description;

    @Column(name = "employee_limit")
    private Integer employeeLimit;

    @Column(name = "custom_employee_limit_allowed", nullable = false)
    private boolean customEmployeeLimitAllowed = false;

    @Column(name = "monthly_price", precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "yearly_price", precision = 10, scale = 2)
    private BigDecimal yearlyPrice;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

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
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getEmployeeLimit() { return employeeLimit; }
    public void setEmployeeLimit(Integer employeeLimit) { this.employeeLimit = employeeLimit; }
    public boolean isCustomEmployeeLimitAllowed() { return customEmployeeLimitAllowed; }
    public void setCustomEmployeeLimitAllowed(boolean customEmployeeLimitAllowed) { this.customEmployeeLimitAllowed = customEmployeeLimitAllowed; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public BigDecimal getYearlyPrice() { return yearlyPrice; }
    public void setYearlyPrice(BigDecimal yearlyPrice) { this.yearlyPrice = yearlyPrice; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
