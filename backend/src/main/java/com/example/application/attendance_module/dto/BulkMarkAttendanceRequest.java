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

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public List<BulkAttendanceEntry> getEntries() { return entries; }
    public void setEntries(List<BulkAttendanceEntry> entries) { this.entries = entries; }
}
