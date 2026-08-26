package com.example.application.employee_assignment_module.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BulkEndAssignmentRequest {
    @NotEmpty(message = "At least one assignment must be selected")
    private List<Long> assignmentIds;

    public List<Long> getAssignmentIds() { return assignmentIds; }
    public void setAssignmentIds(List<Long> assignmentIds) { this.assignmentIds = assignmentIds; }
}
