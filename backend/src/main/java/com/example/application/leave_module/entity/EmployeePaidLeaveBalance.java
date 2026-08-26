package com.example.application.leave_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row per employee per calendar month - the complete accounting for
 * spec section 5: monthlyAllocation, carryForward, extraLeave, usedLeave
 * are kept as SEPARATE columns (never merged), with availableLeave stored
 * as their computed total for fast reads.
 *
 * Generation is idempotent (spec section 14/21#15): EmployeePaidLeaveService
 * .resolveMonth() recomputes monthlyAllocation/carryForward/extraLeave fresh
 * from the current config/grants every time it runs for a given
 * employee+month, and only creates one row per (employee, year, month) -
 * re-processing the same month never double-allocates.
 *
 * usedLeaveDays/manualOverride exist to support future Attendance/Leave-
 * Application integration (spec section 15) without another schema change:
 * a consumer (e.g. the Monthly Attendance & Payment Report) calls
 * recordUsage() to deduct actual leave taken; manualOverride, once set,
 * preserves an admin's direct correction across future recomputations.
 */
@Entity
@Table(name = "employee_paid_leave_balances")
public class EmployeePaidLeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Column(name = "monthly_allocation", nullable = false, precision = 6, scale = 2)
    private BigDecimal monthlyAllocation = BigDecimal.ZERO;

    @Column(name = "carry_forward", nullable = false, precision = 6, scale = 2)
    private BigDecimal carryForward = BigDecimal.ZERO;

    @Column(name = "extra_leave", nullable = false, precision = 6, scale = 2)
    private BigDecimal extraLeave = BigDecimal.ZERO;

    @Column(name = "used_leave", nullable = false, precision = 6, scale = 2)
    private BigDecimal usedLeave = BigDecimal.ZERO;

    @Column(name = "available_leave", nullable = false, precision = 6, scale = 2)
    private BigDecimal availableLeave = BigDecimal.ZERO;

    /** True once usedLeave has been manually corrected (e.g. from the Monthly Report table) rather than auto-recorded. */
    @Column(name = "manual_override", nullable = false)
    private boolean manualOverride = false;

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
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public BigDecimal getMonthlyAllocation() { return monthlyAllocation; }
    public void setMonthlyAllocation(BigDecimal monthlyAllocation) { this.monthlyAllocation = monthlyAllocation; }
    public BigDecimal getCarryForward() { return carryForward; }
    public void setCarryForward(BigDecimal carryForward) { this.carryForward = carryForward; }
    public BigDecimal getExtraLeave() { return extraLeave; }
    public void setExtraLeave(BigDecimal extraLeave) { this.extraLeave = extraLeave; }
    public BigDecimal getUsedLeave() { return usedLeave; }
    public void setUsedLeave(BigDecimal usedLeave) { this.usedLeave = usedLeave; }
    public BigDecimal getAvailableLeave() { return availableLeave; }
    public void setAvailableLeave(BigDecimal availableLeave) { this.availableLeave = availableLeave; }
    public boolean isManualOverride() { return manualOverride; }
    public void setManualOverride(boolean manualOverride) { this.manualOverride = manualOverride; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
