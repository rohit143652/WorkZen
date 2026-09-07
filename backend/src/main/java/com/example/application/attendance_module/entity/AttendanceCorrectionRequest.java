package com.example.application.attendance_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An employee's request to fix their own check-in/check-out-based attendance record for one
 * date. Never applied automatically - see AttendanceCorrectionService.approve(), which re-runs
 * the SAME AttendanceRulesEngine used for a normal check-in/check-out, so an approved correction
 * is computed exactly the same way a normal day would have been, never a raw silent overwrite.
 */
@Entity
@Table(name = "attendance_correction_request")
public class AttendanceCorrectionRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** MISSING_CHECK_IN, MISSING_CHECK_OUT, WRONG_CHECK_IN, WRONG_CHECK_OUT, OTHER */
    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    @Column(name = "requested_check_in")
    private LocalDateTime requestedCheckIn;

    @Column(name = "requested_check_out")
    private LocalDateTime requestedCheckOut;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status = STATUS_PENDING;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_remarks", length = 500)
    private String reviewRemarks;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
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
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewRemarks() { return reviewRemarks; }
    public void setReviewRemarks(String reviewRemarks) { this.reviewRemarks = reviewRemarks; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
