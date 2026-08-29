package com.example.application.employee_module.dto;

import java.util.List;

public class EmployeeBulkImportResult {
    private final int totalRows;
    private final int successCount;
    private final int failureCount;
    private final List<RowError> errors;

    public EmployeeBulkImportResult(int totalRows, int successCount, int failureCount, List<RowError> errors) {
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors;
    }

    public int getTotalRows() { return totalRows; }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
    public List<RowError> getErrors() { return errors; }

    public static class RowError {
        private final int rowNumber;
        private final String reason;

        public RowError(int rowNumber, String reason) {
            this.rowNumber = rowNumber;
            this.reason = reason;
        }

        public int getRowNumber() { return rowNumber; }
        public String getReason() { return reason; }
    }
}
