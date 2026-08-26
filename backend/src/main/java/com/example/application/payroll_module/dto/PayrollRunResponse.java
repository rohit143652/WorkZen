package com.example.application.payroll_module.dto;

import java.time.LocalDateTime;

public class PayrollRunResponse {
    private Long id;
    private int year;
    private int month;
    private String monthLabel;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime calculatedAt;
    private String calculatedBy;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime paidAt;
    private String paidBy;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String cancellationReason;
    private LocalDateTime reopenedAt;
    private String reopenedBy;
    private String reopenReason;
    private PayrollRunSummaryResponse summary;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public String getMonthLabel() { return monthLabel; }
    public void setMonthLabel(String monthLabel) { this.monthLabel = monthLabel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public String getCalculatedBy() { return calculatedBy; }
    public void setCalculatedBy(String calculatedBy) { this.calculatedBy = calculatedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public LocalDateTime getReopenedAt() { return reopenedAt; }
    public void setReopenedAt(LocalDateTime reopenedAt) { this.reopenedAt = reopenedAt; }
    public String getReopenedBy() { return reopenedBy; }
    public void setReopenedBy(String reopenedBy) { this.reopenedBy = reopenedBy; }
    public String getReopenReason() { return reopenReason; }
    public void setReopenReason(String reopenReason) { this.reopenReason = reopenReason; }
    public PayrollRunSummaryResponse getSummary() { return summary; }
    public void setSummary(PayrollRunSummaryResponse summary) { this.summary = summary; }
}
