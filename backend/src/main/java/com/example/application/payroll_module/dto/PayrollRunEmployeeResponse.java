package com.example.application.payroll_module.dto;

import java.math.BigDecimal;

public class PayrollRunEmployeeResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private String designation;
    private String siteName;
    private String salaryStructureName;
    private String salaryType;

    private int totalCalendarDays;
    private int presentDays;
    private int halfDays;
    private int onLeaveDays;
    private int absentDays;
    private BigDecimal paidLeaveDays;
    private BigDecimal unpaidLeaveDays;
    private BigDecimal payableDays;
    private BigDecimal leaveBalanceClosing;

    private BigDecimal basicSalary;
    private BigDecimal da;
    private BigDecimal grossSalary;

    private BigDecimal allowance;
    private BigDecimal totalEarnings;

    private BigDecimal epfEmployee;
    private BigDecimal epfEmployer;
    private BigDecimal epfEmployeePercentUsed;
    private BigDecimal epfEmployerPercentUsed;
    private BigDecimal esiEmployee;
    private BigDecimal esiEmployer;
    private BigDecimal esiEmployeePercentUsed;
    private BigDecimal esiEmployerPercentUsed;
    private BigDecimal professionalTax;
    private BigDecimal otherManualDeduction;
    private BigDecimal advanceRecovery;
    private BigDecimal totalDeductions;

    private BigDecimal advanceOutstandingBeforeRecovery;
    private BigDecimal advanceOutstandingAfterRecovery;

    private BigDecimal totalSalaryCtc;
    private BigDecimal netPay;
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getSalaryStructureName() { return salaryStructureName; }
    public void setSalaryStructureName(String salaryStructureName) { this.salaryStructureName = salaryStructureName; }
    public String getSalaryType() { return salaryType; }
    public void setSalaryType(String salaryType) { this.salaryType = salaryType; }
    public int getTotalCalendarDays() { return totalCalendarDays; }
    public void setTotalCalendarDays(int totalCalendarDays) { this.totalCalendarDays = totalCalendarDays; }
    public int getPresentDays() { return presentDays; }
    public void setPresentDays(int presentDays) { this.presentDays = presentDays; }
    public int getHalfDays() { return halfDays; }
    public void setHalfDays(int halfDays) { this.halfDays = halfDays; }
    public int getOnLeaveDays() { return onLeaveDays; }
    public void setOnLeaveDays(int onLeaveDays) { this.onLeaveDays = onLeaveDays; }
    public int getAbsentDays() { return absentDays; }
    public void setAbsentDays(int absentDays) { this.absentDays = absentDays; }
    public BigDecimal getPaidLeaveDays() { return paidLeaveDays; }
    public void setPaidLeaveDays(BigDecimal paidLeaveDays) { this.paidLeaveDays = paidLeaveDays; }
    public BigDecimal getUnpaidLeaveDays() { return unpaidLeaveDays; }
    public void setUnpaidLeaveDays(BigDecimal unpaidLeaveDays) { this.unpaidLeaveDays = unpaidLeaveDays; }
    public BigDecimal getPayableDays() { return payableDays; }
    public void setPayableDays(BigDecimal payableDays) { this.payableDays = payableDays; }
    public BigDecimal getLeaveBalanceClosing() { return leaveBalanceClosing; }
    public void setLeaveBalanceClosing(BigDecimal leaveBalanceClosing) { this.leaveBalanceClosing = leaveBalanceClosing; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public BigDecimal getDa() { return da; }
    public void setDa(BigDecimal da) { this.da = da; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }
    public BigDecimal getAllowance() { return allowance; }
    public void setAllowance(BigDecimal allowance) { this.allowance = allowance; }
    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }
    public BigDecimal getEpfEmployee() { return epfEmployee; }
    public void setEpfEmployee(BigDecimal epfEmployee) { this.epfEmployee = epfEmployee; }
    public BigDecimal getEpfEmployer() { return epfEmployer; }
    public void setEpfEmployer(BigDecimal epfEmployer) { this.epfEmployer = epfEmployer; }
    public BigDecimal getEpfEmployeePercentUsed() { return epfEmployeePercentUsed; }
    public void setEpfEmployeePercentUsed(BigDecimal epfEmployeePercentUsed) { this.epfEmployeePercentUsed = epfEmployeePercentUsed; }
    public BigDecimal getEpfEmployerPercentUsed() { return epfEmployerPercentUsed; }
    public void setEpfEmployerPercentUsed(BigDecimal epfEmployerPercentUsed) { this.epfEmployerPercentUsed = epfEmployerPercentUsed; }
    public BigDecimal getEsiEmployee() { return esiEmployee; }
    public void setEsiEmployee(BigDecimal esiEmployee) { this.esiEmployee = esiEmployee; }
    public BigDecimal getEsiEmployer() { return esiEmployer; }
    public void setEsiEmployer(BigDecimal esiEmployer) { this.esiEmployer = esiEmployer; }
    public BigDecimal getEsiEmployeePercentUsed() { return esiEmployeePercentUsed; }
    public void setEsiEmployeePercentUsed(BigDecimal esiEmployeePercentUsed) { this.esiEmployeePercentUsed = esiEmployeePercentUsed; }
    public BigDecimal getEsiEmployerPercentUsed() { return esiEmployerPercentUsed; }
    public void setEsiEmployerPercentUsed(BigDecimal esiEmployerPercentUsed) { this.esiEmployerPercentUsed = esiEmployerPercentUsed; }
    public BigDecimal getProfessionalTax() { return professionalTax; }
    public void setProfessionalTax(BigDecimal professionalTax) { this.professionalTax = professionalTax; }
    public BigDecimal getOtherManualDeduction() { return otherManualDeduction; }
    public void setOtherManualDeduction(BigDecimal otherManualDeduction) { this.otherManualDeduction = otherManualDeduction; }
    public BigDecimal getAdvanceRecovery() { return advanceRecovery; }
    public void setAdvanceRecovery(BigDecimal advanceRecovery) { this.advanceRecovery = advanceRecovery; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    public BigDecimal getAdvanceOutstandingBeforeRecovery() { return advanceOutstandingBeforeRecovery; }
    public void setAdvanceOutstandingBeforeRecovery(BigDecimal advanceOutstandingBeforeRecovery) { this.advanceOutstandingBeforeRecovery = advanceOutstandingBeforeRecovery; }
    public BigDecimal getAdvanceOutstandingAfterRecovery() { return advanceOutstandingAfterRecovery; }
    public void setAdvanceOutstandingAfterRecovery(BigDecimal advanceOutstandingAfterRecovery) { this.advanceOutstandingAfterRecovery = advanceOutstandingAfterRecovery; }
    public BigDecimal getTotalSalaryCtc() { return totalSalaryCtc; }
    public void setTotalSalaryCtc(BigDecimal totalSalaryCtc) { this.totalSalaryCtc = totalSalaryCtc; }
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
