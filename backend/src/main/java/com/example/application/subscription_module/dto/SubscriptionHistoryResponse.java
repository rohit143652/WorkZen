package com.example.application.subscription_module.dto;

import java.time.LocalDateTime;

public class SubscriptionHistoryResponse {
    private Long id;
    private String previousPlanName;
    private String newPlanName;
    private String previousBillingCycle;
    private String newBillingCycle;
    private Integer previousEmployeeLimit;
    private Integer newEmployeeLimit;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime changeDate;
    private String reason;
    private String changedByUsername;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPreviousPlanName() { return previousPlanName; }
    public void setPreviousPlanName(String previousPlanName) { this.previousPlanName = previousPlanName; }
    public String getNewPlanName() { return newPlanName; }
    public void setNewPlanName(String newPlanName) { this.newPlanName = newPlanName; }
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
    public String getChangedByUsername() { return changedByUsername; }
    public void setChangedByUsername(String changedByUsername) { this.changedByUsername = changedByUsername; }
}
