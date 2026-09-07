package com.example.application.attendance_module.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * One row per tenant (UNIQUE client_company_id) - company-wide attendance policy instead of
 * hardcoded constants anywhere in Java. See AttendanceRulesEngine for where these are actually
 * applied.
 */
@Entity
@Table(name = "attendance_rule_config")
public class AttendanceRuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_company_id", nullable = false)
    private Long clientCompanyId;

    @Column(name = "office_start_time", nullable = false)
    private LocalTime officeStartTime;

    @Column(name = "office_end_time", nullable = false)
    private LocalTime officeEndTime;

    @Column(name = "required_working_minutes", nullable = false)
    private Integer requiredWorkingMinutes;

    @Column(name = "half_day_min_minutes", nullable = false)
    private Integer halfDayMinMinutes;

    @Column(name = "full_day_min_minutes", nullable = false)
    private Integer fullDayMinMinutes;

    @Column(name = "late_grace_minutes", nullable = false)
    private Integer lateGraceMinutes;

    @Column(name = "default_break_minutes", nullable = false)
    private Integer defaultBreakMinutes;

    @Column(name = "allow_multiple_checkin", nullable = false)
    private boolean allowMultipleCheckin;

    /** Comma-separated java.time.DayOfWeek names, e.g. "SATURDAY,SUNDAY". */
    @Column(name = "weekly_off_days", nullable = false, length = 100)
    private String weeklyOffDays;

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
    public LocalTime getOfficeStartTime() { return officeStartTime; }
    public void setOfficeStartTime(LocalTime officeStartTime) { this.officeStartTime = officeStartTime; }
    public LocalTime getOfficeEndTime() { return officeEndTime; }
    public void setOfficeEndTime(LocalTime officeEndTime) { this.officeEndTime = officeEndTime; }
    public Integer getRequiredWorkingMinutes() { return requiredWorkingMinutes; }
    public void setRequiredWorkingMinutes(Integer requiredWorkingMinutes) { this.requiredWorkingMinutes = requiredWorkingMinutes; }
    public Integer getHalfDayMinMinutes() { return halfDayMinMinutes; }
    public void setHalfDayMinMinutes(Integer halfDayMinMinutes) { this.halfDayMinMinutes = halfDayMinMinutes; }
    public Integer getFullDayMinMinutes() { return fullDayMinMinutes; }
    public void setFullDayMinMinutes(Integer fullDayMinMinutes) { this.fullDayMinMinutes = fullDayMinMinutes; }
    public Integer getLateGraceMinutes() { return lateGraceMinutes; }
    public void setLateGraceMinutes(Integer lateGraceMinutes) { this.lateGraceMinutes = lateGraceMinutes; }
    public Integer getDefaultBreakMinutes() { return defaultBreakMinutes; }
    public void setDefaultBreakMinutes(Integer defaultBreakMinutes) { this.defaultBreakMinutes = defaultBreakMinutes; }
    public boolean isAllowMultipleCheckin() { return allowMultipleCheckin; }
    public void setAllowMultipleCheckin(boolean allowMultipleCheckin) { this.allowMultipleCheckin = allowMultipleCheckin; }
    public String getWeeklyOffDays() { return weeklyOffDays; }
    public void setWeeklyOffDays(String weeklyOffDays) { this.weeklyOffDays = weeklyOffDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
