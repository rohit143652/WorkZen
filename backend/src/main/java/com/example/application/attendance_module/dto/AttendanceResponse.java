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
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Integer grossWorkMinutes;
    private Integer breakMinutes;
    private Integer netWorkMinutes;
    private String workMode;
    private String attendanceSource;
    private boolean late;
    private Integer lateMinutes;
    private boolean earlyExit;
    private Integer earlyExitMinutes;
    private String createdByRole;
    private String modifiedByRole;
    private String modificationReason;

    public String getCreatedByRole() { return createdByRole; }
    public void setCreatedByRole(String createdByRole) { this.createdByRole = createdByRole; }
    public String getModifiedByRole() { return modifiedByRole; }
    public void setModifiedByRole(String modifiedByRole) { this.modifiedByRole = modifiedByRole; }
    public String getModificationReason() { return modificationReason; }
    public void setModificationReason(String modificationReason) { this.modificationReason = modificationReason; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public Integer getGrossWorkMinutes() { return grossWorkMinutes; }
    public void setGrossWorkMinutes(Integer grossWorkMinutes) { this.grossWorkMinutes = grossWorkMinutes; }
    public Integer getBreakMinutes() { return breakMinutes; }
    public void setBreakMinutes(Integer breakMinutes) { this.breakMinutes = breakMinutes; }
    public Integer getNetWorkMinutes() { return netWorkMinutes; }
    public void setNetWorkMinutes(Integer netWorkMinutes) { this.netWorkMinutes = netWorkMinutes; }
    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }
    public String getAttendanceSource() { return attendanceSource; }
    public void setAttendanceSource(String attendanceSource) { this.attendanceSource = attendanceSource; }
    public boolean isLate() { return late; }
    public void setLate(boolean late) { this.late = late; }
    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }
    public boolean isEarlyExit() { return earlyExit; }
    public void setEarlyExit(boolean earlyExit) { this.earlyExit = earlyExit; }
    public Integer getEarlyExitMinutes() { return earlyExitMinutes; }
    public void setEarlyExitMinutes(Integer earlyExitMinutes) { this.earlyExitMinutes = earlyExitMinutes; }

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
