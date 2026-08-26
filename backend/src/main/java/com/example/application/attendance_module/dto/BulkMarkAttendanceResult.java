package com.example.application.attendance_module.dto;

import java.util.List;

public class BulkMarkAttendanceResult {
    private int requested;
    private int marked;
    private List<String> rejected;

    public BulkMarkAttendanceResult(int requested, int marked, List<String> rejected) {
        this.requested = requested;
        this.marked = marked;
        this.rejected = rejected;
    }

    public int getRequested() { return requested; }
    public int getMarked() { return marked; }
    public List<String> getRejected() { return rejected; }
}
