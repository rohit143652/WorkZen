package com.example.application.attendance_module.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Long siteId;
    private String siteName;
    private LocalDate attendanceDate;
    private String status;
    private String remarks;
    private String markedByUsername;
    private String updatedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Whether the CURRENT caller is allowed to edit this row (has ATTENDANCE_UPDATE) - lets the UI show/hide an Edit action without a second round trip. */
    private boolean editable;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getMarkedByUsername() { return markedByUsername; }
    public void setMarkedByUsername(String markedByUsername) { this.markedByUsername = markedByUsername; }
    public String getUpdatedByUsername() { return updatedByUsername; }
    public void setUpdatedByUsername(String updatedByUsername) { this.updatedByUsername = updatedByUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
}
