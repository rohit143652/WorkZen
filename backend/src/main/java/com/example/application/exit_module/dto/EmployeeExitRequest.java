package com.example.application.exit_module.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class EmployeeExitRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate resignationDate;

    @NotNull
    private LocalDate lastWorkingDay;

    private String reason;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getResignationDate() { return resignationDate; }
    public void setResignationDate(LocalDate resignationDate) { this.resignationDate = resignationDate; }
    public LocalDate getLastWorkingDay() { return lastWorkingDay; }
    public void setLastWorkingDay(LocalDate lastWorkingDay) { this.lastWorkingDay = lastWorkingDay; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
