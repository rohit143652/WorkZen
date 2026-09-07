package com.example.application.attendance_module.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CorrectionRequestResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate attendanceDate;
    private String requestType;
    private LocalDateTime requestedCheckIn;
    private LocalDateTime requestedCheckOut;
    private String reason;
    private String status;
    private String reviewedByUsername;
    private LocalDateTime reviewedAt;
    private String reviewRemarks;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewedByUsername() { return reviewedByUsername; }
    public void setReviewedByUsername(String reviewedByUsername) { this.reviewedByUsername = reviewedByUsername; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewRemarks() { return reviewRemarks; }
    public void setReviewRemarks(String reviewRemarks) { this.reviewRemarks = reviewRemarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
