package com.example.application.payroll_module.dto;

import java.math.BigDecimal;

/**
 * Everything PayrollCalculationService needs to compute one employee's
 * earnings/deductions/Net Pay for one month - deliberately plain values
 * (no Employee/PayrollSettings entity types), so this engine has no
 * dependency on how the caller resolved eligibility/rates. The caller
 * (PayrollRunService.calculateRun()) is responsible for combining
 * tenant-level PayrollSettings with the employee's own pfApplicable/
 * esiApplicable/ptApplicable flags into the single *Applicable booleans
 * here - "never assume every employee has PF/ESI/PT" is enforced by the
 * caller passing false when either the tenant or the employee opts out.
 *
 * Built via {@link #builder()} rather than a long positional constructor
 * (architecture refactor Phase 6 added fullGrossEntitlement/bonus/
 * overtime/arrears on top of the already-sizeable set of fields from
 * Phases 1-5 - a builder is far safer against argument-order mistakes).
 */
public class PayrollCalculationInput {
    private Long tenantId;
    private Long employeeId;
    private int year;
    private int month;
    /** Which PayrollRun this calculation belongs to - threaded through to AdvanceRecoveryTransaction for audit ("which payroll recovered this?"). Null if calculated outside a persisted run (not currently possible, but kept nullable for safety). */
    private Long payrollRunId;

    private BigDecimal basicSalary;
    private BigDecimal da;
    /** Attendance-prorated Gross for the month - null when the employee has no active Salary Structure this month. This is what PF/ESI are based on and what caps Advance Recovery room. */
    private BigDecimal totalGross;
    /** The FULL entitled Gross if present every working day (architecture refactor Phase 6) - the gap between this and totalGross becomes the "Unpaid Leave Deduction" line. Defaults to totalGross (i.e. no unpaid-leave gap) if left unset. */
    private BigDecimal fullGrossEntitlement;

    private boolean pfApplicable;
    private BigDecimal epfEmployeePercent;
    private BigDecimal epfEmployerPercent;

    private boolean esiApplicable;
    private BigDecimal esiEmployeePercent;
    private BigDecimal esiEmployerPercent;
    /** Null = no ceiling. */
    private BigDecimal esiWageCeiling;

    private boolean ptApplicable;
    private BigDecimal professionalTaxAmount;

    private BigDecimal otherManualDeduction = BigDecimal.ZERO;
    private BigDecimal allowance = BigDecimal.ZERO;
    private BigDecimal bonus = BigDecimal.ZERO;
    private BigDecimal overtime = BigDecimal.ZERO;
    private BigDecimal arrears = BigDecimal.ZERO;

    private PayrollCalculationInput() { }

    public static Builder builder() { return new Builder(); }

    public Long getTenantId() { return tenantId; }
    public Long getEmployeeId() { return employeeId; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public Long getPayrollRunId() { return payrollRunId; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public BigDecimal getDa() { return da; }
    public BigDecimal getTotalGross() { return totalGross; }
    public BigDecimal getFullGrossEntitlement() { return fullGrossEntitlement; }
    public boolean isPfApplicable() { return pfApplicable; }
    public BigDecimal getEpfEmployeePercent() { return epfEmployeePercent; }
    public BigDecimal getEpfEmployerPercent() { return epfEmployerPercent; }
    public boolean isEsiApplicable() { return esiApplicable; }
    public BigDecimal getEsiEmployeePercent() { return esiEmployeePercent; }
    public BigDecimal getEsiEmployerPercent() { return esiEmployerPercent; }
    public BigDecimal getEsiWageCeiling() { return esiWageCeiling; }
    public boolean isPtApplicable() { return ptApplicable; }
    public BigDecimal getProfessionalTaxAmount() { return professionalTaxAmount; }
    public BigDecimal getOtherManualDeduction() { return otherManualDeduction; }
    public BigDecimal getAllowance() { return allowance; }
    public BigDecimal getBonus() { return bonus; }
    public BigDecimal getOvertime() { return overtime; }
    public BigDecimal getArrears() { return arrears; }

    public static final class Builder {
        private final PayrollCalculationInput target = new PayrollCalculationInput();

        public Builder tenantId(Long v) { target.tenantId = v; return this; }
        public Builder employeeId(Long v) { target.employeeId = v; return this; }
        public Builder year(int v) { target.year = v; return this; }
        public Builder month(int v) { target.month = v; return this; }
        public Builder payrollRunId(Long v) { target.payrollRunId = v; return this; }
        public Builder basicSalary(BigDecimal v) { target.basicSalary = v; return this; }
        public Builder da(BigDecimal v) { target.da = v; return this; }
        public Builder totalGross(BigDecimal v) { target.totalGross = v; return this; }
        public Builder fullGrossEntitlement(BigDecimal v) { target.fullGrossEntitlement = v; return this; }
        public Builder pf(boolean applicable, BigDecimal employeePercent, BigDecimal employerPercent) {
            target.pfApplicable = applicable; target.epfEmployeePercent = employeePercent; target.epfEmployerPercent = employerPercent; return this;
        }
        public Builder esi(boolean applicable, BigDecimal employeePercent, BigDecimal employerPercent, BigDecimal wageCeiling) {
            target.esiApplicable = applicable; target.esiEmployeePercent = employeePercent; target.esiEmployerPercent = employerPercent; target.esiWageCeiling = wageCeiling; return this;
        }
        public Builder pt(boolean applicable, BigDecimal amount) {
            target.ptApplicable = applicable; target.professionalTaxAmount = amount; return this;
        }
        public Builder otherManualDeduction(BigDecimal v) { target.otherManualDeduction = v == null ? BigDecimal.ZERO : v; return this; }
        public Builder allowance(BigDecimal v) { target.allowance = v == null ? BigDecimal.ZERO : v; return this; }
        public Builder bonus(BigDecimal v) { target.bonus = v == null ? BigDecimal.ZERO : v; return this; }
        public Builder overtime(BigDecimal v) { target.overtime = v == null ? BigDecimal.ZERO : v; return this; }
        public Builder arrears(BigDecimal v) { target.arrears = v == null ? BigDecimal.ZERO : v; return this; }

        public PayrollCalculationInput build() {
            if (target.fullGrossEntitlement == null) {
                target.fullGrossEntitlement = target.totalGross;
            }
            return target;
        }
    }
}
