package com.example.application.advance_module.dto;

import jakarta.validation.constraints.NotNull;

public class AdvanceRecoverViaPayrollRequest {
    @NotNull(message = "recoverViaPayroll is required")
    private Boolean recoverViaPayroll;

    public Boolean getRecoverViaPayroll() { return recoverViaPayroll; }
    public void setRecoverViaPayroll(Boolean recoverViaPayroll) { this.recoverViaPayroll = recoverViaPayroll; }
}
