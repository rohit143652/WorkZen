package com.example.application.payroll_module.dto;

import java.math.BigDecimal;

/** Always computed at read time from persisted PayrollRunEmployee rows - never stored/duplicated on PayrollRun itself, so it can never drift out of sync. */
public class PayrollRunSummaryResponse {
    private long totalEmployees;
    private BigDecimal totalGross;
    private BigDecimal totalEarnings;
    private BigDecimal totalEpf;
    private BigDecimal totalEsi;
    private BigDecimal totalPt;
    private BigDecimal totalOtherDeduction;
    private BigDecimal totalAdvanceRecovery;
    private BigDecimal totalDeductions;
    private BigDecimal totalNetPay;

    public PayrollRunSummaryResponse(long totalEmployees, BigDecimal totalGross, BigDecimal totalEarnings,
                                      BigDecimal totalEpf, BigDecimal totalEsi, BigDecimal totalPt,
                                      BigDecimal totalOtherDeduction, BigDecimal totalAdvanceRecovery,
                                      BigDecimal totalDeductions, BigDecimal totalNetPay) {
        this.totalEmployees = totalEmployees;
        this.totalGross = totalGross;
        this.totalEarnings = totalEarnings;
        this.totalEpf = totalEpf;
        this.totalEsi = totalEsi;
        this.totalPt = totalPt;
        this.totalOtherDeduction = totalOtherDeduction;
        this.totalAdvanceRecovery = totalAdvanceRecovery;
        this.totalDeductions = totalDeductions;
        this.totalNetPay = totalNetPay;
    }

    public long getTotalEmployees() { return totalEmployees; }
    public BigDecimal getTotalGross() { return totalGross; }
    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public BigDecimal getTotalEpf() { return totalEpf; }
    public BigDecimal getTotalEsi() { return totalEsi; }
    public BigDecimal getTotalPt() { return totalPt; }
    public BigDecimal getTotalOtherDeduction() { return totalOtherDeduction; }
    public BigDecimal getTotalAdvanceRecovery() { return totalAdvanceRecovery; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public BigDecimal getTotalNetPay() { return totalNetPay; }
}
