package com.example.application.attendance_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MarkAttendanceRequest {
    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotNull(message = "Attendance date is required")
    @PastOrPresent(message = "Attendance date cannot be in the future")
    private LocalDate attendanceDate;

    @NotBlank(message = "Status is required")
    private String status;

    private String remarks;

    /** Optional - when BOTH are supplied, working hours/late/early-exit/status are computed by
        the same AttendanceRulesEngine a self check-out uses, exactly like the spec's "Case A:
        Manual Attendance With Check-In and Check-Out" (calculation must not differ by source).
        Leave both null for "Case B" (e.g. plain "mark Present, no times") - the service never
        invents a timestamp to fill these in. */
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String workMode;

    /** The marking device's current GPS position - optional (older/non-GPS clients still work), but if the assigned site has a geofence configured, AttendanceService requires these to be present and within range. Never used for anything except that one check plus an audit record on the saved Attendance row. */
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
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
    public java.math.BigDecimal getLatitude() { return latitude; }
    public void setLatitude(java.math.BigDecimal latitude) { this.latitude = latitude; }
    public java.math.BigDecimal getLongitude() { return longitude; }
    public void setLongitude(java.math.BigDecimal longitude) { this.longitude = longitude; }
}
