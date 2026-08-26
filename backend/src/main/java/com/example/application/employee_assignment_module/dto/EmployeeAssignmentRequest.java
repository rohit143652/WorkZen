package com.example.application.employee_assignment_module.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class EmployeeAssignmentRequest {
    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotNull(message = "Site is required")
    private Long siteId;

    private String assignmentType = "REGULAR";

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private boolean primary = true;
    private String remarks;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public String getAssignmentType() { return assignmentType; }
    public void setAssignmentType(String assignmentType) { this.assignmentType = assignmentType; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
