package com.example.application.employee_assignment_module.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BulkEmployeeAssignmentRequest {
    @NotNull(message = "Site is required")
    private Long siteId;

    @NotEmpty(message = "At least one employee must be selected")
    private List<Long> employeeIds;

    @NotNull(message = "Start date is required")
    private java.time.LocalDate startDate;

    private String remarks;

    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public List<Long> getEmployeeIds() { return employeeIds; }
    public void setEmployeeIds(List<Long> employeeIds) { this.employeeIds = employeeIds; }
    public java.time.LocalDate getStartDate() { return startDate; }
    public void setStartDate(java.time.LocalDate startDate) { this.startDate = startDate; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
