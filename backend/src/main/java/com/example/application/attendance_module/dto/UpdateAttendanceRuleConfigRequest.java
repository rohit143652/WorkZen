package com.example.application.attendance_module.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class UpdateAttendanceRuleConfigRequest {

    @NotNull(message = "Office start time is required")
    private LocalTime officeStartTime;

    @NotNull(message = "Office end time is required")
    private LocalTime officeEndTime;

    @NotNull(message = "Required working minutes is required")
    private Integer requiredWorkingMinutes;

    @NotNull(message = "Half day minimum minutes is required")
    private Integer halfDayMinMinutes;

    @NotNull(message = "Full day minimum minutes is required")
    private Integer fullDayMinMinutes;

    @NotNull(message = "Late grace minutes is required")
    private Integer lateGraceMinutes;

    @NotNull(message = "Default break minutes is required")
    private Integer defaultBreakMinutes;

    private boolean allowMultipleCheckin;
    private boolean checkInSelfieRequired;
    private boolean checkOutSelfieRequired;

    /** Comma-separated DayOfWeek names, e.g. "SATURDAY,SUNDAY". */
    private String weeklyOffDays;

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
    public boolean isCheckInSelfieRequired() { return checkInSelfieRequired; }
    public void setCheckInSelfieRequired(boolean checkInSelfieRequired) { this.checkInSelfieRequired = checkInSelfieRequired; }
    public boolean isCheckOutSelfieRequired() { return checkOutSelfieRequired; }
    public void setCheckOutSelfieRequired(boolean checkOutSelfieRequired) { this.checkOutSelfieRequired = checkOutSelfieRequired; }
    public String getWeeklyOffDays() { return weeklyOffDays; }
    public void setWeeklyOffDays(String weeklyOffDays) { this.weeklyOffDays = weeklyOffDays; }
}
