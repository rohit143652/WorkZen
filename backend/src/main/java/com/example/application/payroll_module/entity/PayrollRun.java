package com.example.application.payroll_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One persisted payroll processing run for one tenant + one calendar
 * month (architecture refactor Phase 2). Prior to this, "payroll" was
 * only ever a transient response of viewing the Monthly Attendance &
 * Payment Report - re-opening that report could silently regenerate
 * different numbers and mutate leave/advance data as a side effect. A
 * PayrollRun makes calculation an explicit, one-time, auditable action:
 * DRAFT (created) -> CALCULATED (employees processed) -> APPROVED -> PAID,
 * with CANCELLED as a separate terminal state, and a controlled reopen
 * path back from APPROVED to CALCULATED (architecture refactor Phase 7).
 * See PayrollRunService / PayrollStatusTransitionService for the exact
 * status-transition rules.
 */
@Entity
@Table(name = "payroll_runs")
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    /** DRAFT, CALCULATED, APPROVED, PAID, CANCELLED - see PayrollRunService for the allowed transitions. */
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(length = 500)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "calculated_by")
    private Long calculatedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_by")
    private Long paidBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @Column(name = "reopened_by")
    private Long reopenedBy;

    @Column(name = "reopen_reason", length = 500)
    private String reopenReason;

    /** Optimistic locking (architecture refactor Phase 7) - guards against two admins approving/recalculating the same run at once; see PayrollRunService. No other entity in the project uses @Version yet - introduced here specifically because PayrollRun is the one place a lost-update race genuinely matters. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public Long getCalculatedBy() { return calculatedBy; }
    public void setCalculatedBy(Long calculatedBy) { this.calculatedBy = calculatedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public Long getPaidBy() { return paidBy; }
    public void setPaidBy(Long paidBy) { this.paidBy = paidBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public Long getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(Long cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public LocalDateTime getReopenedAt() { return reopenedAt; }
    public void setReopenedAt(LocalDateTime reopenedAt) { this.reopenedAt = reopenedAt; }
    public Long getReopenedBy() { return reopenedBy; }
    public void setReopenedBy(Long reopenedBy) { this.reopenedBy = reopenedBy; }
    public String getReopenReason() { return reopenReason; }
    public void setReopenReason(String reopenReason) { this.reopenReason = reopenReason; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
