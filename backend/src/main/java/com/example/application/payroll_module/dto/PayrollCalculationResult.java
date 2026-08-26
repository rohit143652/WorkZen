package com.example.application.payroll_module.dto;

import java.math.BigDecimal;

/** Everything PayrollCalculationService computed for one employee/month - the numbers a report or a future PayrollRunEmployee row would display/store. */
public class PayrollCalculationResult {
    private final BigDecimal basicSalary;
    private final BigDecimal da;
    private final BigDecimal totalGross;
    private final BigDecimal epfEmployee;
    private final BigDecimal epfEmployer;
    private final BigDecimal esiEmployee;
    private final BigDecimal esiEmployer;
    private final BigDecimal totalSalary;
    private final BigDecimal professionalTax;
    private final BigDecimal otherManualDeduction;
    private final BigDecimal advanceRecovery;
    private final BigDecimal outstandingAdvance;
    private final BigDecimal allowance;
    private final BigDecimal totalDeduct;
    private final BigDecimal netPayment;

    public PayrollCalculationResult(BigDecimal basicSalary, BigDecimal da, BigDecimal totalGross,
                                     BigDecimal epfEmployee, BigDecimal epfEmployer,
                                     BigDecimal esiEmployee, BigDecimal esiEmployer,
                                     BigDecimal totalSalary, BigDecimal professionalTax,
                                     BigDecimal otherManualDeduction, BigDecimal advanceRecovery, BigDecimal outstandingAdvance,
                                     BigDecimal allowance, BigDecimal totalDeduct, BigDecimal netPayment) {
        this.basicSalary = basicSalary;
        this.da = da;
        this.totalGross = totalGross;
        this.epfEmployee = epfEmployee;
        this.epfEmployer = epfEmployer;
        this.esiEmployee = esiEmployee;
        this.esiEmployer = esiEmployer;
        this.totalSalary = totalSalary;
        this.professionalTax = professionalTax;
        this.otherManualDeduction = otherManualDeduction;
        this.advanceRecovery = advanceRecovery;
        this.outstandingAdvance = outstandingAdvance;
        this.allowance = allowance;
        this.totalDeduct = totalDeduct;
        this.netPayment = netPayment;
    }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public BigDecimal getDa() { return da; }
    public BigDecimal getTotalGross() { return totalGross; }
    public BigDecimal getEpfEmployee() { return epfEmployee; }
    public BigDecimal getEpfEmployer() { return epfEmployer; }
    public BigDecimal getEsiEmployee() { return esiEmployee; }
    public BigDecimal getEsiEmployer() { return esiEmployer; }
    public BigDecimal getTotalSalary() { return totalSalary; }
    public BigDecimal getProfessionalTax() { return professionalTax; }
    public BigDecimal getOtherManualDeduction() { return otherManualDeduction; }
    public BigDecimal getAdvanceRecovery() { return advanceRecovery; }
    public BigDecimal getOutstandingAdvance() { return outstandingAdvance; }
    public BigDecimal getAllowance() { return allowance; }
    public BigDecimal getTotalDeduct() { return totalDeduct; }
    public BigDecimal getNetPayment() { return netPayment; }
}
