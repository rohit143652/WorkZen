package com.example.application.advance_module.dto;

import java.math.BigDecimal;

/** Tenant-wide totals for the Advance Dashboard - always computed at read time from persisted EmployeeAdvance/AdvanceRecoveryTransaction data, never stored separately. */
public class AdvanceDashboardSummaryResponse {
    private long totalAdvancesCount;
    private BigDecimal totalAdvancesGiven;
    private BigDecimal totalRecovered;
    private BigDecimal totalOutstanding;
    private BigDecimal currentMonthRecovery;

    public AdvanceDashboardSummaryResponse(long totalAdvancesCount, BigDecimal totalAdvancesGiven, BigDecimal totalRecovered,
                                            BigDecimal totalOutstanding, BigDecimal currentMonthRecovery) {
        this.totalAdvancesCount = totalAdvancesCount;
        this.totalAdvancesGiven = totalAdvancesGiven;
        this.totalRecovered = totalRecovered;
        this.totalOutstanding = totalOutstanding;
        this.currentMonthRecovery = currentMonthRecovery;
    }

    public long getTotalAdvancesCount() { return totalAdvancesCount; }
    public BigDecimal getTotalAdvancesGiven() { return totalAdvancesGiven; }
    public BigDecimal getTotalRecovered() { return totalRecovered; }
    public BigDecimal getTotalOutstanding() { return totalOutstanding; }
    public BigDecimal getCurrentMonthRecovery() { return currentMonthRecovery; }
}
