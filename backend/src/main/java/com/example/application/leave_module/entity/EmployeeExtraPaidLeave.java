package com.example.application.leave_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A one-time additional Paid Leave grant (spec section 3/4/6) - e.g. 30 days
 * of Medical Leave - kept completely separate from the monthly allocation
 * (spec section 16). Never overwritten: cancelling one (status=CANCELLED)
 * preserves the row for history; edits create no new row, they just update
 * this one (still visible with its original createdAt/createdBy).
 *
 * Contributes to EmployeePaidLeaveBalance.extraLeave for exactly the
 * calendar month of startDate (see EmployeePaidLeaveService.resolveMonth) -
 * after that it flows forward automatically via the normal carry-forward
 * mechanism, so it is never double-counted across the months it spans.
 */
@Entity
@Table(name = "employee_extra_paid_leaves")
public class EmployeeExtraPaidLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "leave_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal leaveDays;

    /** MEDICAL, SPECIAL, EMERGENCY, OTHER */
    @Column(nullable = false, length = 20)
    private String reason;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 255)
    private String remark;

    /** ACTIVE or CANCELLED. */
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
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public BigDecimal getLeaveDays() { return leaveDays; }
    public void setLeaveDays(BigDecimal leaveDays) { this.leaveDays = leaveDays; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
