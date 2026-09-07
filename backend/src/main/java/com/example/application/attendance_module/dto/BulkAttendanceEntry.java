package com.example.application.attendance_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class BulkAttendanceEntry {
    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotBlank(message = "status is required")
    private String status;

    private String remarks;

    /** Optional - see MarkAttendanceRequest for the "both present = computed via AttendanceRulesEngine" behavior. */
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String workMode;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }
}
