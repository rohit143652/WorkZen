package com.example.application.payroll_module.dto;

import jakarta.validation.constraints.NotBlank;

public class PayrollRunReopenRequest {
    @NotBlank(message = "reopenReason is required")
    private String reopenReason;

    public String getReopenReason() { return reopenReason; }
    public void setReopenReason(String reopenReason) { this.reopenReason = reopenReason; }
}
