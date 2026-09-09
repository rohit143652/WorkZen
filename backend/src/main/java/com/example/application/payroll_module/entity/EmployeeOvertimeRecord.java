package com.example.application.payroll_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One day's overtime for one employee - the actual "who did overtime, on which day, how many
 * hours" record (see V107 migration javadoc for why this replaced a single monthly number).
 * A month's total overtime hours for payroll is always SUMMED from these rows at calculation
 * time (see PayrollRunService) - never stored as a separate aggregate, so it can never drift
 * out of sync with the underlying day-wise entries, same principle PayrollRunSummaryResponse
 * already follows for its own totals.
 */
@Entity
@Table(name = "employee_overtime_records")
public class EmployeeOvertimeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "overtime_date", nullable = false)
    private LocalDate overtimeDate;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal hours;

    /** The actual rupee amount for this day's overtime, entered directly by whoever logs it -
        NOT hours x a fixed company-wide rate (see V108 migration for why that rate-based
        approach was dropped: different days/situations can legitimately warrant different
        amounts, e.g. holiday overtime vs a weekday). This is what PayrollRunService sums for
        the month's Overtime line - hours above is kept purely as the informational "how long"
        record alongside it. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String remarks;

    /** Who logged this entry - an Admin/Supervisor/HR (matching ADMIN_MANUAL_ATTENDANCE-style roles), not the employee themselves - there is no employee self-service overtime entry, only self-service correction REQUESTS exist for attendance, and overtime has no equivalent yet. */
    @Column(name = "marked_by")
    private Long markedBy;

    @Column(name = "marked_by_role", length = 30)
    private String markedByRole;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getOvertimeDate() { return overtimeDate; }
    public void setOvertimeDate(LocalDate overtimeDate) { this.overtimeDate = overtimeDate; }
    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Long getMarkedBy() { return markedBy; }
    public void setMarkedBy(Long markedBy) { this.markedBy = markedBy; }
    public String getMarkedByRole() { return markedByRole; }
    public void setMarkedByRole(String markedByRole) { this.markedByRole = markedByRole; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
