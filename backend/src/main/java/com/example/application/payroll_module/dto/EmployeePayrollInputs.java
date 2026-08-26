package com.example.application.payroll_module.dto;

import java.math.BigDecimal;

/**
 * What one employee's attendance/leave/salary-structure data resolves to
 * for a given month, BEFORE any deduction math runs. Produced by
 * payroll_module.PayrollInputResolver and consumed by both the legacy
 * Monthly Attendance & Payment Report and the new persisted Payroll Run -
 * this is the shared "gathering" step so that logic exists in exactly one
 * place, the same way PayrollCalculationService is the one place the
 * deduction/net-pay math runs (Phase 1).
 */
public class EmployeePayrollInputs {
    private long presentDays;
    private long halfDays;
    private long onLeaveDays;
    private long absentDays;
    private BigDecimal paidLeaveDays;
    private BigDecimal unpaidLeaveDays;
    private BigDecimal payableDays;
    private BigDecimal basicSalary;
    private BigDecimal da;
    /** Null when the employee has no active Salary Structure this month. */
    private BigDecimal totalGross;
    /**
     * The FULL entitled Gross for the month if the employee had been present every working
     * day - i.e. totalGross before attendance proration (architecture refactor Phase 6). The
     * gap between this and totalGross is exactly what PayrollCalculationService reports as the
     * "Unpaid Leave Deduction" line, so Total Earnings (this figure + bonus/overtime/arrears)
     * and Total Deductions (this deduction + PF/ESI/PT/other/advance) both reconcile cleanly to
     * the same Net Pay the pre-Phase-6 single-prorated-gross formula already produced.
     */
    private BigDecimal fullGrossEntitlement;
    private BigDecimal rate;
    private String structureName;
    private String salaryType;
    private String note = "";
    private BigDecimal leaveBalanceOpening;
    private BigDecimal leaveBalanceClosing;
    private boolean manualLeaveOverride;

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
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public BigDecimal getDa() { return da; }
    public void setDa(BigDecimal da) { this.da = da; }
    public BigDecimal getTotalGross() { return totalGross; }
    public void setTotalGross(BigDecimal totalGross) { this.totalGross = totalGross; }
    public BigDecimal getFullGrossEntitlement() { return fullGrossEntitlement; }
    public void setFullGrossEntitlement(BigDecimal fullGrossEntitlement) { this.fullGrossEntitlement = fullGrossEntitlement; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public String getStructureName() { return structureName; }
    public void setStructureName(String structureName) { this.structureName = structureName; }
    public String getSalaryType() { return salaryType; }
    public void setSalaryType(String salaryType) { this.salaryType = salaryType; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public BigDecimal getLeaveBalanceOpening() { return leaveBalanceOpening; }
    public void setLeaveBalanceOpening(BigDecimal leaveBalanceOpening) { this.leaveBalanceOpening = leaveBalanceOpening; }
    public BigDecimal getLeaveBalanceClosing() { return leaveBalanceClosing; }
    public void setLeaveBalanceClosing(BigDecimal leaveBalanceClosing) { this.leaveBalanceClosing = leaveBalanceClosing; }
    public boolean isManualLeaveOverride() { return manualLeaveOverride; }
    public void setManualLeaveOverride(boolean manualLeaveOverride) { this.manualLeaveOverride = manualLeaveOverride; }
}
