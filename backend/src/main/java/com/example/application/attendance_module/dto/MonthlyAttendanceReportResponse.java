package com.example.application.attendance_module.dto;

import java.util.List;

/** Architecture refactor Phase 4: no monetary total here anymore - this report has no money in it at all. Payroll totals live on payroll_module.PayrollRunResponse's summary, computed from persisted PayrollRunEmployee rows. */
public class MonthlyAttendanceReportResponse {
    private int year;
    private int month;
    private String monthLabel;
    private int daysInMonth;
    private List<MonthlyAttendanceReportRow> rows;

    public MonthlyAttendanceReportResponse(int year, int month, String monthLabel, int daysInMonth,
                                            List<MonthlyAttendanceReportRow> rows) {
        this.year = year;
        this.month = month;
        this.monthLabel = monthLabel;
        this.daysInMonth = daysInMonth;
        this.rows = rows;
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public String getMonthLabel() { return monthLabel; }
    public int getDaysInMonth() { return daysInMonth; }
    public List<MonthlyAttendanceReportRow> getRows() { return rows; }
}
