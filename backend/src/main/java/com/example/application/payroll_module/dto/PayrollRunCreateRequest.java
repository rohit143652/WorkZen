package com.example.application.payroll_module.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PayrollRunCreateRequest {
    @NotNull(message = "year is required")
    private Integer year;

    @NotNull(message = "month is required")
    @Min(value = 1, message = "month must be between 1 and 12")
    @Max(value = 12, message = "month must be between 1 and 12")
    private Integer month;

    private String remarks;

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
