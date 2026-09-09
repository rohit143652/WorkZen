package com.example.application.payroll_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row per employee per calendar month for the manually-entered payroll
 * figures that have no other source of truth in this system (unlike
 * EPF/ESI/PT, which are formula-derived from PayrollSettings, and unlike
 * Gross Earnings, which comes from Salary Structure): a manual Other
 * Deduction (e.g. Uniform, Canteen, Fine/Penalty), an Allowance, and
 * (architecture refactor Phase 6) Bonus/Overtime/Arrears - each kept as
 * its own explicit column so PayrollCalculationService's breakdown can
 * show every one separately rather than folding them into one generic
 * number. Upserted the same way as EmployeePaidLeaveBalance - editing an
 * employee+month here never affects any other month.
 */
@Entity
@Table(name = "employee_payroll_adjustments")
public class EmployeePayrollAdjustment {

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

    @Column(name = "other_manual_deduction", nullable = false, precision = 10, scale = 2)
    private BigDecimal otherManualDeduction = BigDecimal.ZERO;

    @Column(name = "allowance", nullable = false, precision = 10, scale = 2)
    private BigDecimal allowance = BigDecimal.ZERO;

    /** Architecture refactor Phase 6: additional earnings, kept as their own explicit fields (never merged into otherManualDeduction/allowance) so Total Earnings can show each separately. */
    @Column(name = "bonus", nullable = false, precision = 10, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    /** Legacy, unused - see V105 migration comment. Overtime pay is now derived from overtimeHours x PayrollSettings.overtimeRatePerHour instead of a raw amount, so nothing writes to this column any more; kept only so any historical row that somehow had a value doesn't lose it. */
    @Column(name = "overtime", nullable = false, precision = 10, scale = 2)
    private BigDecimal overtime = BigDecimal.ZERO;

    /** Legacy, unused - superseded by the Overtime Register (EmployeeOvertimeRecord, see V107
        migration) within the same release that introduced this column. A month's overtime
        hours for payroll are now summed from that day-wise, auditable log instead of being
        typed in here as one number - kept in place rather than dropped, same as the sibling
        'overtime' amount column above, since nothing is actually lost by leaving an unused
        column at its default of 0.00. */
    @Column(name = "overtime_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "arrears", nullable = false, precision = 10, scale = 2)
    private BigDecimal arrears = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

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
    public BigDecimal getOtherManualDeduction() { return otherManualDeduction; }
    public void setOtherManualDeduction(BigDecimal otherManualDeduction) { this.otherManualDeduction = otherManualDeduction; }
    public BigDecimal getAllowance() { return allowance; }
    public void setAllowance(BigDecimal allowance) { this.allowance = allowance; }
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
    public BigDecimal getOvertime() { return overtime; }
    public void setOvertime(BigDecimal overtime) { this.overtime = overtime; }
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
    public BigDecimal getArrears() { return arrears; }
    public void setArrears(BigDecimal arrears) { this.arrears = arrears; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
