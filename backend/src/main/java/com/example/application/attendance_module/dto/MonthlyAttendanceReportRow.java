package com.example.application.attendance_module.dto;

import java.math.BigDecimal;

/**
 * One employee's attendance/leave FACTS for a month - architecture
 * refactor Phase 4: this row deliberately contains NO money. Gross Salary,
 * PF, ESI, PT, Tax, Advance Recovery, and Net Pay all live exclusively in
 * payroll_module.PayrollRunEmployee, populated only by an explicit Payroll
 * Run calculation - never by viewing this report. See
 * MonthlyAttendanceReportService for how paidLeaveDays/unpaidLeaveDays are
 * resolved read-only (without writing to the Paid Leave module).
 *
 * A brief experiment surfaced read-only Gross/Deductions/Net Pay figures
 * here too (copied from an already-calculated PayrollRunEmployee, never
 * computed here) - removed on request, restoring this class to containing
 * attendance/leave facts only. See Payroll Run Details / the Salary
 * Register export for those figures instead.
 */
public class MonthlyAttendanceReportRow {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private String designation;
    private String currentSite;
    private long presentDays;
    private long halfDays;
    private long onLeaveDays;
    private long absentDays;
    private BigDecimal paidLeaveDays;
    private BigDecimal unpaidLeaveDays;
    private BigDecimal payableDays;
    private BigDecimal leaveBalanceOpening;
    private BigDecimal leaveBalanceClosing;
    private boolean manualLeaveOverride;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getCurrentSite() { return currentSite; }
    public void setCurrentSite(String currentSite) { this.currentSite = currentSite; }
    public long getPresentDays() { return presentDays; }
    public void setPresentDays(long presentDays) { this.presentDays = presentDays; }
    public long getHalfDays() { return halfDays; }
    public void setHalfDays(long halfDays) { this.halfDays = halfDays; }
    public long getOnLeaveDays() { return onLeaveDays; }
    public void setOnLeaveDays(long onLeaveDays) { this.onLeaveDays = onLeaveDays; }
    public long getAbsentDays() { return absentDays; }
    public void setAbsentDays(long absentDays) { this.absentDays = absentDays; }
    public BigDecimal getPaidLeaveDays() { return paidLeaveDays; }
    public void setPaidLeaveDays(BigDecimal paidLeaveDays) { this.paidLeaveDays = paidLeaveDays; }
    public BigDecimal getUnpaidLeaveDays() { return unpaidLeaveDays; }
    public void setUnpaidLeaveDays(BigDecimal unpaidLeaveDays) { this.unpaidLeaveDays = unpaidLeaveDays; }
    public BigDecimal getPayableDays() { return payableDays; }
    public void setPayableDays(BigDecimal payableDays) { this.payableDays = payableDays; }
    public BigDecimal getLeaveBalanceOpening() { return leaveBalanceOpening; }
    public void setLeaveBalanceOpening(BigDecimal leaveBalanceOpening) { this.leaveBalanceOpening = leaveBalanceOpening; }
    public BigDecimal getLeaveBalanceClosing() { return leaveBalanceClosing; }
    public void setLeaveBalanceClosing(BigDecimal leaveBalanceClosing) { this.leaveBalanceClosing = leaveBalanceClosing; }
    public boolean isManualLeaveOverride() { return manualLeaveOverride; }
    public void setManualLeaveOverride(boolean manualLeaveOverride) { this.manualLeaveOverride = manualLeaveOverride; }
}
