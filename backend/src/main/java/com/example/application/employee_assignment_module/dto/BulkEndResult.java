package com.example.application.employee_assignment_module.dto;

import java.util.List;

public class BulkEndResult {
    private int requested;
    private int ended;
    private List<String> failed;

    public BulkEndResult(int requested, int ended, List<String> failed) {
        this.requested = requested;
        this.ended = ended;
        this.failed = failed;
    }

    public int getRequested() { return requested; }
    public int getEnded() { return ended; }
    public List<String> getFailed() { return failed; }
}
