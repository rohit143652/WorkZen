package com.example.application.payroll_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One employee's persisted monthly payroll result, snapshotted at the
 * moment PayrollRunService.calculate() ran - the whole point of this
 * table is that it NEVER changes just because attendance, leave, salary
 * structure, or PF/ESI/PT settings change later (spec: "August payroll
 * must continue to show 12%" even if the tenant's PF rate changes in
 * September). Only an explicit recalculation (allowed while the parent
 * PayrollRun is still DRAFT/CALCULATED, blocked once APPROVED/PAID - see
 * PayrollRunService) ever overwrites a row here.
 *
 * Deliberately does NOT break gross pay down into HRA/Conveyance/Special
 * Allowance/Bonus/Overtime/Arrears - PayrollCalculationService (Phase 1)
 * doesn't compute those individually yet, it only produces one lump Gross
 * figure from the employee's Salary Structure. Adding placeholder columns
 * for calculations the engine doesn't perform would misrepresent what
 * this snapshot actually explains; those fields belong here once the
 * engine itself is extended to produce them, not before.
 *
 * employeeCode/employeeName/department/designation/siteName/
 * salaryStructureName are denormalized on purpose: this is a historical
 * snapshot, so it must keep showing "what was true that month" even if
 * the employee is later renamed, reassigned, or their structure changed.
 */
@Entity
@Table(name = "payroll_run_employees")
public class PayrollRunEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payroll_run_id", nullable = false)
    private Long payrollRunId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "employee_name", nullable = false, length = 200)
    private String employeeName;

    @Column(length = 150)
    private String department;

    @Column(length = 150)
    private String designation;

    @Column(name = "site_name", length = 150)
    private String siteName;

    @Column(name = "salary_structure_name", length = 150)
    private String salaryStructureName;

    /** MONTHLY, DAILY, HOURLY, CONTRACT - null if no structure was assigned that month. */
    @Column(name = "salary_type", length = 20)
    private String salaryType;

    // ---- Attendance snapshot ----
    @Column(name = "total_calendar_days", nullable = false)
    private int totalCalendarDays;

    @Column(name = "present_days", nullable = false)
    private int presentDays;

    @Column(name = "half_days", nullable = false)
    private int halfDays;

    @Column(name = "on_leave_days", nullable = false)
    private int onLeaveDays;

    @Column(name = "absent_days", nullable = false)
    private int absentDays;

    @Column(name = "paid_leave_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal paidLeaveDays;

    @Column(name = "unpaid_leave_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal unpaidLeaveDays;

    @Column(name = "payable_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal payableDays;

    /** Informational only - the Paid Leave module's own balance/history is still the source of truth for leave itself. */
    @Column(name = "leave_balance_closing", precision = 6, scale = 2)
    private BigDecimal leaveBalanceClosing;

    // ---- Salary snapshot ----
    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal da;

    /** The employee's Gross Pay for the month, prorated by payable days - see PayrollCalculationService. */
    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;

    // ---- Earnings ----
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal allowance;

    @Column(name = "total_earnings", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEarnings;

    // ---- Deductions ----
    @Column(name = "epf_employee", nullable = false, precision = 12, scale = 2)
    private BigDecimal epfEmployee;

    @Column(name = "epf_employer", nullable = false, precision = 12, scale = 2)
    private BigDecimal epfEmployer;

    /** The ACTUAL rate applied (architecture refactor Phase 8) - null when PF/ESI wasn't applicable that month. Explains a historical payslip even after PayrollSettings has since changed - see PayrollSettingsResolver. */
    @Column(name = "epf_employee_percent_used", precision = 5, scale = 2)
    private BigDecimal epfEmployeePercentUsed;

    @Column(name = "epf_employer_percent_used", precision = 5, scale = 2)
    private BigDecimal epfEmployerPercentUsed;

    @Column(name = "esi_employee", nullable = false, precision = 12, scale = 2)
    private BigDecimal esiEmployee;

    @Column(name = "esi_employer", nullable = false, precision = 12, scale = 2)
    private BigDecimal esiEmployer;

    @Column(name = "esi_employee_percent_used", precision = 5, scale = 2)
    private BigDecimal esiEmployeePercentUsed;

    @Column(name = "esi_employer_percent_used", precision = 5, scale = 2)
    private BigDecimal esiEmployerPercentUsed;

    @Column(name = "professional_tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal professionalTax;

    /** Manually-entered deduction (uniform, etc.) - see EmployeePayrollAdjustment; kept separate from real EmployeeAdvance recovery. */
    @Column(name = "other_manual_deduction", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherManualDeduction;

    @Column(name = "advance_recovery", nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceRecovery;

    @Column(name = "total_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDeductions;

    // ---- Advance snapshot ----
    @Column(name = "advance_outstanding_before_recovery", nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceOutstandingBeforeRecovery;

    @Column(name = "advance_outstanding_after_recovery", nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceOutstandingAfterRecovery;

    // ---- Final ----
    /** Total Salary / CTC = Gross + Employer PF + Employer ESI (informational - not paid to the employee). */
    @Column(name = "total_salary_ctc", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSalaryCtc;

    @Column(name = "net_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal netPay;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPayrollRunId() { return payrollRunId; }
    public void setPayrollRunId(Long payrollRunId) { this.payrollRunId = payrollRunId; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
