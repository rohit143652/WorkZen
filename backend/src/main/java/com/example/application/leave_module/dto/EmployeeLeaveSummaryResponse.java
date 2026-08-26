package com.example.application.leave_module.dto;

import java.math.BigDecimal;

public class EmployeeLeaveSummaryResponse {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private BigDecimal availableLeave;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public BigDecimal getAvailableLeave() { return availableLeave; }
    public void setAvailableLeave(BigDecimal availableLeave) { this.availableLeave = availableLeave; }
}
