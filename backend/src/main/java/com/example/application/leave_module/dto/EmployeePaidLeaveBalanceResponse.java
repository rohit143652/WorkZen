package com.example.application.leave_module.dto;

import java.math.BigDecimal;

/**
 * Spec section 5: monthlyAllocation/carryForward/extraLeave/usedLeave are
 * always shown separately, never merged into one figure.
 */
public class EmployeePaidLeaveBalanceResponse {
    private int year;
    private int month;
    private BigDecimal monthlyAllocation;
    private BigDecimal carryForward;
    private BigDecimal extraLeave;
    private BigDecimal usedLeave;
    private BigDecimal availableLeave;
    private boolean manualOverride;

    public EmployeePaidLeaveBalanceResponse(int year, int month, BigDecimal monthlyAllocation, BigDecimal carryForward,
                                             BigDecimal extraLeave, BigDecimal usedLeave, BigDecimal availableLeave,
                                             boolean manualOverride) {
        this.year = year;
        this.month = month;
        this.monthlyAllocation = monthlyAllocation;
        this.carryForward = carryForward;
        this.extraLeave = extraLeave;
        this.usedLeave = usedLeave;
        this.availableLeave = availableLeave;
        this.manualOverride = manualOverride;
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public BigDecimal getMonthlyAllocation() { return monthlyAllocation; }
    public BigDecimal getCarryForward() { return carryForward; }
    public BigDecimal getExtraLeave() { return extraLeave; }
    public BigDecimal getUsedLeave() { return usedLeave; }
    public BigDecimal getAvailableLeave() { return availableLeave; }
    public boolean isManualOverride() { return manualOverride; }
}
