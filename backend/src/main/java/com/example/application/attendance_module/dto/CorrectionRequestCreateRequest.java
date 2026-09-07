package com.example.application.attendance_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CorrectionRequestCreateRequest {

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotBlank(message = "Request type is required")
    private String requestType;

    private LocalDateTime requestedCheckIn;
    private LocalDateTime requestedCheckOut;

    @NotBlank(message = "A reason is required")
    private String reason;

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public LocalDateTime getRequestedCheckIn() { return requestedCheckIn; }
    public void setRequestedCheckIn(LocalDateTime requestedCheckIn) { this.requestedCheckIn = requestedCheckIn; }
    public LocalDateTime getRequestedCheckOut() { return requestedCheckOut; }
    public void setRequestedCheckOut(LocalDateTime requestedCheckOut) { this.requestedCheckOut = requestedCheckOut; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
