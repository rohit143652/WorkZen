package com.example.application.attendance_module.dto;

/** One row in the "mark attendance" screen: an employee eligible for attendance, plus any existing record for the selected date. */
public class EmployeeAttendanceOption {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Long siteId;
    private String siteName;
    private AttendanceResponse existingRecord; // null if not yet marked for the selected date

    public EmployeeAttendanceOption(Long employeeId, String employeeCode, String employeeName, Long siteId,
                                     String siteName, AttendanceResponse existingRecord) {
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.siteId = siteId;
        this.siteName = siteName;
        this.existingRecord = existingRecord;
    }

    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeCode() { return employeeCode; }
    public String getEmployeeName() { return employeeName; }
    public Long getSiteId() { return siteId; }
    public String getSiteName() { return siteName; }
    public AttendanceResponse getExistingRecord() { return existingRecord; }
}
