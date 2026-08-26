package com.example.application.leave_module.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class PaidLeaveConfigRequest {
    /** Required when scheduling a new policy (POST); ignored when editing a not-yet-effective one (PUT), which keeps its original effective date. */
    private LocalDate effectiveFrom;

    @NotNull(message = "monthlyPaidLeave is required")
    @Min(value = 0, message = "monthlyPaidLeave must be >= 0")
    private Integer monthlyPaidLeave;

    /** Master switch (spec: client decides whether Paid Leave is active or fully off for this scheduled period). */
    @NotNull(message = "enabled is required")
    private Boolean enabled;

    @NotNull(message = "allowCarryForward is required")
    private Boolean allowCarryForward;

    /** Null = unlimited. */
    @Min(value = 0, message = "maximumCarryForward must be >= 0")
    private Integer maximumCarryForward;

    @NotNull(message = "resetAnnually is required")
    private Boolean resetAnnually;

    public Integer getMonthlyPaidLeave() { return monthlyPaidLeave; }
    public void setMonthlyPaidLeave(Integer monthlyPaidLeave) { this.monthlyPaidLeave = monthlyPaidLeave; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Boolean getAllowCarryForward() { return allowCarryForward; }
    public void setAllowCarryForward(Boolean allowCarryForward) { this.allowCarryForward = allowCarryForward; }
    public Integer getMaximumCarryForward() { return maximumCarryForward; }
    public void setMaximumCarryForward(Integer maximumCarryForward) { this.maximumCarryForward = maximumCarryForward; }
    public Boolean getResetAnnually() { return resetAnnually; }
    public void setResetAnnually(Boolean resetAnnually) { this.resetAnnually = resetAnnually; }
}
