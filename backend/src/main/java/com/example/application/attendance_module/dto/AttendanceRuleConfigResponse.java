package com.example.application.attendance_module.dto;

import java.time.LocalTime;

public class AttendanceRuleConfigResponse {
    private Long id;
    private LocalTime officeStartTime;
    private LocalTime officeEndTime;
    private Integer requiredWorkingMinutes;
    private Integer halfDayMinMinutes;
    private Integer fullDayMinMinutes;
    private Integer lateGraceMinutes;
    private Integer defaultBreakMinutes;
    private boolean allowMultipleCheckin;
    private String weeklyOffDays;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
}
