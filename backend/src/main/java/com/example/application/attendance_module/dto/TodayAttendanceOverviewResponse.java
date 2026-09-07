package com.example.application.attendance_module.dto;

import java.time.LocalDate;
import java.util.List;

public class TodayAttendanceOverviewResponse {
    private LocalDate date;
    private long totalActiveEmployees;
    private long checkedInCount;
    private long notCheckedInCount;
    private long onLeaveCount;
    private List<Entry> checkedIn;
    private List<Entry> notCheckedIn;

    public static class Entry {
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private String checkInTime; // ISO string, null for the not-checked-in list
        private String checkOutTime; // ISO string, null while still checked in (i.e. currently working)
        private String workMode;
        private boolean late;

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public String getEmployeeCode() { return employeeCode; }
        public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getCheckInTime() { return checkInTime; }
        public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }
        public String getCheckOutTime() { return checkOutTime; }
        public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }
        public String getWorkMode() { return workMode; }
        public void setWorkMode(String workMode) { this.workMode = workMode; }
        public boolean isLate() { return late; }
        public void setLate(boolean late) { this.late = late; }
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public long getTotalActiveEmployees() { return totalActiveEmployees; }
    public void setTotalActiveEmployees(long totalActiveEmployees) { this.totalActiveEmployees = totalActiveEmployees; }
    public long getCheckedInCount() { return checkedInCount; }
    public void setCheckedInCount(long checkedInCount) { this.checkedInCount = checkedInCount; }
    public long getNotCheckedInCount() { return notCheckedInCount; }
    public void setNotCheckedInCount(long notCheckedInCount) { this.notCheckedInCount = notCheckedInCount; }
    public long getOnLeaveCount() { return onLeaveCount; }
    public void setOnLeaveCount(long onLeaveCount) { this.onLeaveCount = onLeaveCount; }
    public List<Entry> getCheckedIn() { return checkedIn; }
    public void setCheckedIn(List<Entry> checkedIn) { this.checkedIn = checkedIn; }
    public List<Entry> getNotCheckedIn() { return notCheckedIn; }
    public void setNotCheckedIn(List<Entry> notCheckedIn) { this.notCheckedIn = notCheckedIn; }
}
