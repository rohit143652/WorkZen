package com.example.application.attendance_module.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;

public class BulkMarkAttendanceRequest {
    @NotNull(message = "Attendance date is required")
    @PastOrPresent(message = "Attendance date cannot be in the future")
    private LocalDate attendanceDate;

    @NotEmpty(message = "At least one entry is required")
    @Valid
    private List<BulkAttendanceEntry> entries;

    /** Captured once for the whole batch, from the marking device (the supervisor's own phone/browser) - not per-employee, since one submission comes from one device at one point in time. Checked against EACH employee's own assigned site individually (see AttendanceService.checkGeofence()), since a bulk batch can span employees at different sites. */
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public List<BulkAttendanceEntry> getEntries() { return entries; }
    public void setEntries(List<BulkAttendanceEntry> entries) { this.entries = entries; }
    public java.math.BigDecimal getLatitude() { return latitude; }
    public void setLatitude(java.math.BigDecimal latitude) { this.latitude = latitude; }
    public java.math.BigDecimal getLongitude() { return longitude; }
    public void setLongitude(java.math.BigDecimal longitude) { this.longitude = longitude; }
}
