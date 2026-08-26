package com.example.application.leave_module.dto;

import java.time.LocalDate;

public class PaidLeaveConfigResponse {
    private Long id;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
    private int monthlyPaidLeave;
    private boolean enabled;
    private boolean allowCarryForward;
    private Integer maximumCarryForward;
    private boolean resetAnnually;

    public PaidLeaveConfigResponse(int monthlyPaidLeave, boolean enabled, boolean allowCarryForward, Integer maximumCarryForward, boolean resetAnnually) {
        this.monthlyPaidLeave = monthlyPaidLeave;
        this.enabled = enabled;
        this.allowCarryForward = allowCarryForward;
        this.maximumCarryForward = maximumCarryForward;
        this.resetAnnually = resetAnnually;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getMonthlyPaidLeave() { return monthlyPaidLeave; }
    public boolean isEnabled() { return enabled; }
    public boolean isAllowCarryForward() { return allowCarryForward; }
    public Integer getMaximumCarryForward() { return maximumCarryForward; }
    public boolean isResetAnnually() { return resetAnnually; }
}
