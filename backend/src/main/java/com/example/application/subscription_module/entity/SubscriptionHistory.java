package com.example.application.subscription_module.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Append-only log entry for one subscription change (plan, cycle, limit, or status). Never updated after creation - see ClientSubscriptionService for where these get written. */
@Entity
@Table(name = "subscription_history")
public class SubscriptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "previous_plan_id")
    private Long previousPlanId;

    @Column(name = "new_plan_id")
    private Long newPlanId;

    @Column(name = "previous_billing_cycle", length = 20)
    private String previousBillingCycle;

    @Column(name = "new_billing_cycle", length = 20)
    private String newBillingCycle;

    @Column(name = "previous_employee_limit")
    private Integer previousEmployeeLimit;

    @Column(name = "new_employee_limit")
    private Integer newEmployeeLimit;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate = LocalDateTime.now();

    @Column(length = 255)
    private String reason;

    @Column(name = "changed_by")
    private Long changedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public Long getPreviousPlanId() { return previousPlanId; }
    public void setPreviousPlanId(Long previousPlanId) { this.previousPlanId = previousPlanId; }
    public Long getNewPlanId() { return newPlanId; }
    public void setNewPlanId(Long newPlanId) { this.newPlanId = newPlanId; }
    public String getPreviousBillingCycle() { return previousBillingCycle; }
    public void setPreviousBillingCycle(String previousBillingCycle) { this.previousBillingCycle = previousBillingCycle; }
    public String getNewBillingCycle() { return newBillingCycle; }
    public void setNewBillingCycle(String newBillingCycle) { this.newBillingCycle = newBillingCycle; }
    public Integer getPreviousEmployeeLimit() { return previousEmployeeLimit; }
    public void setPreviousEmployeeLimit(Integer previousEmployeeLimit) { this.previousEmployeeLimit = previousEmployeeLimit; }
    public Integer getNewEmployeeLimit() { return newEmployeeLimit; }
    public void setNewEmployeeLimit(Integer newEmployeeLimit) { this.newEmployeeLimit = newEmployeeLimit; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public LocalDateTime getChangeDate() { return changeDate; }
    public void setChangeDate(LocalDateTime changeDate) { this.changeDate = changeDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getChangedBy() { return changedBy; }
    public void setChangedBy(Long changedBy) { this.changedBy = changedBy; }
}
