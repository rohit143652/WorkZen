package com.example.application.attendance_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per employee per calendar date. Immutable by design once created
 * for SITE_ADMIN/SITE_SUPERVISOR - see AttendanceService: only a holder of
 * ATTENDANCE_UPDATE (CLIENT_ADMIN by default grant) can call the edit
 * endpoint at all; the create endpoint refuses to touch an existing row
 * outright, so there is no code path where a non-editor can silently
 * overwrite a previously marked day.
 */
@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /** The site the employee was assigned to at the time attendance was marked. */
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** PRESENT, ABSENT, HALF_DAY, ON_LEAVE */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 255)
    private String remarks;

    /** The marking device's GPS position at the moment this was saved - a permanent audit record, purely informational (e.g. to review a disputed mark later). Never recomputed or reused afterward. */
    private java.math.BigDecimal markedLatitude;
    private java.math.BigDecimal markedLongitude;

    @Column(name = "marked_by")
    private Long markedBy;

    @Column(name = "updated_by")
    private Long updatedBy;

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
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public java.math.BigDecimal getMarkedLatitude() { return markedLatitude; }
    public void setMarkedLatitude(java.math.BigDecimal markedLatitude) { this.markedLatitude = markedLatitude; }
    public java.math.BigDecimal getMarkedLongitude() { return markedLongitude; }
    public void setMarkedLongitude(java.math.BigDecimal markedLongitude) { this.markedLongitude = markedLongitude; }
    public Long getMarkedBy() { return markedBy; }
    public void setMarkedBy(Long markedBy) { this.markedBy = markedBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
