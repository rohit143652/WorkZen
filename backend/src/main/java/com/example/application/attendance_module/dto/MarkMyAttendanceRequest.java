package com.example.application.attendance_module.dto;

import java.math.BigDecimal;

/** For "Mark My Attendance" (self-service) - deliberately has NO employeeId, date, or status fields; the employee is always the caller's own login, the date is always today, and the status is always PRESENT (see AttendanceService.markMine()). Only the device's GPS position (if any) is provided here. */
public class MarkMyAttendanceRequest {
    private BigDecimal latitude;
    private BigDecimal longitude;

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}
