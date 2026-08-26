package com.example.application.payroll_module.dto;

import jakarta.validation.constraints.NotBlank;

public class PayrollRunCancelRequest {
    @NotBlank(message = "cancellationReason is required")
    private String cancellationReason;

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}
