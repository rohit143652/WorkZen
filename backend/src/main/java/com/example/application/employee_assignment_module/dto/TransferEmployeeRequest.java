package com.example.application.employee_assignment_module.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class TransferEmployeeRequest {
    @NotNull(message = "Destination site is required")
    private Long toSiteId;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private String reason;

    public Long getToSiteId() { return toSiteId; }
    public void setToSiteId(Long toSiteId) { this.toSiteId = toSiteId; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
