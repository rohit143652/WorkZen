package com.example.application.employee_assignment_module.dto;

import java.util.List;

public class BulkAssignmentResult {
    private int requested;
    private int assigned;
    private List<String> rejected; // human-readable reasons, e.g. "Employee 105: already assigned"

    public BulkAssignmentResult(int requested, int assigned, List<String> rejected) {
        this.requested = requested;
        this.assigned = assigned;
        this.rejected = rejected;
    }

    public int getRequested() { return requested; }
    public int getAssigned() { return assigned; }
    public List<String> getRejected() { return rejected; }
}
