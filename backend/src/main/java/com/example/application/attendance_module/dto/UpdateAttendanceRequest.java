package com.example.application.attendance_module.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class UpdateAttendanceRequest {
    @NotBlank(message = "Status is required")
    private String status;

    private String remarks;

    /** Optional - supplying both recomputes hours/late/early-exit/status via AttendanceRulesEngine, same as a normal check-out. */
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    /** Why this correction is being made - stored as attendance.modification_reason for the audit trail. */
    private String modificationReason;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public String getModificationReason() { return modificationReason; }
    public void setModificationReason(String modificationReason) { this.modificationReason = modificationReason; }
}
