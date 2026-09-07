package com.example.application.attendance_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per employee per calendar date - the single centralized attendance record for that
 * employee/day, however it was created. Two ways to populate one:
 *
 * 1. The original one-click flow (mark()/bulkMark()/markMine()): sets status directly, leaves
 *    checkInTime/checkOutTime/etc. null. Immutable by design once created for SITE_ADMIN/
 *    SITE_SUPERVISOR - only a holder of ATTENDANCE_UPDATE (CLIENT_ADMIN by default grant) can
 *    call edit() at all; the create endpoint refuses to touch an existing row outright.
 *
 * 2. The check-in/check-out flow (AttendanceService.checkIn()/checkOut()): populates the time-
 *    tracking fields and derives status/late/early-exit automatically via AttendanceRulesEngine
 *    using the tenant's AttendanceRuleConfig. A correction to one of these goes through
 *    AttendanceCorrectionRequest instead of a raw edit - see that entity/service.
 *
 * Both flows write to this SAME table/entity - there is deliberately no separate "employee
 * attendance" vs. "admin attendance" table, per the centralized-record design.
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

    @Column(name = "check_out_latitude")
    private java.math.BigDecimal checkOutLatitude;
    @Column(name = "check_out_longitude")
    private java.math.BigDecimal checkOutLongitude;

    /** All fields below this point are optional (null) for any row created through the original
        one-click mark()/bulkMark()/markMine() flow - that flow is untouched and keeps working
        exactly as before for anyone who doesn't check in/out with actual times. They're only
        populated when the row goes through the newer AttendanceService.checkIn()/checkOut()
        path (see that class and AttendanceRulesEngine for how they're computed). */
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "gross_work_minutes")
    private Integer grossWorkMinutes;

    @Column(name = "break_minutes")
    private Integer breakMinutes;

    @Column(name = "net_work_minutes")
    private Integer netWorkMinutes;

    /** OFFICE, WORK_FROM_HOME, FIELD_WORK - null for rows from the old one-click flow. */
    @Column(name = "work_mode", length = 20)
    private String workMode;

    /** SELF, ADMIN, SYSTEM, BIOMETRIC, QR, API - defaults to ADMIN at the DB level (see V96) so
        every pre-existing row has a value; SELF is set explicitly wherever the employee acted on
        their own behalf (checkIn()/checkOut()/markMine()). */
    @Column(name = "attendance_source", nullable = false, length = 20)
    private String attendanceSource = "ADMIN";

    @Column(name = "is_late", nullable = false)
    private boolean late;

    @Column(name = "late_minutes")
    private Integer lateMinutes;

    @Column(name = "is_early_exit", nullable = false)
    private boolean earlyExit;

    @Column(name = "early_exit_minutes")
    private Integer earlyExitMinutes;

    @Column(name = "marked_by")
    private Long markedBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    /** The ACTOR's role at the moment they created/modified this record (SELF isn't a role - for
        self check-in/out this mirrors attendanceSource; for manual marks it's whichever admin-
        tier role actually performed the action, e.g. SITE_SUPERVISOR vs CLIENT_ADMIN, which
        attendanceSource alone can't distinguish since it's just "ADMIN"). */
    @Column(name = "created_by_role", length = 30)
    private String createdByRole;

    @Column(name = "modified_by_role", length = 30)
    private String modifiedByRole;

    /** Why a correction was made - set on edit()/correction-approval, never on the original create. */
    @Column(name = "modification_reason", length = 255)
    private String modificationReason;

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
    public String getCreatedByRole() { return createdByRole; }
    public void setCreatedByRole(String createdByRole) { this.createdByRole = createdByRole; }
    public String getModifiedByRole() { return modifiedByRole; }
    public void setModifiedByRole(String modifiedByRole) { this.modifiedByRole = modifiedByRole; }
    public String getModificationReason() { return modificationReason; }
    public void setModificationReason(String modificationReason) { this.modificationReason = modificationReason; }
    public java.math.BigDecimal getCheckOutLatitude() { return checkOutLatitude; }
    public void setCheckOutLatitude(java.math.BigDecimal checkOutLatitude) { this.checkOutLatitude = checkOutLatitude; }
    public java.math.BigDecimal getCheckOutLongitude() { return checkOutLongitude; }
    public void setCheckOutLongitude(java.math.BigDecimal checkOutLongitude) { this.checkOutLongitude = checkOutLongitude; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
