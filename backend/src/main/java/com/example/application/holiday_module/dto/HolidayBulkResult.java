package com.example.application.holiday_module.dto;

import java.util.List;

public class HolidayBulkResult {
    private final int totalRequested;
    private final int successCount;
    private final int failureCount;
    private final List<ItemError> errors;

    public HolidayBulkResult(int totalRequested, int successCount, int failureCount, List<ItemError> errors) {
        this.totalRequested = totalRequested;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors;
    }

    public int getTotalRequested() { return totalRequested; }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
    public List<ItemError> getErrors() { return errors; }

    public static class ItemError {
        private final String name;
        private final String reason;

        public ItemError(String name, String reason) {
            this.name = name;
            this.reason = reason;
        }

        public String getName() { return name; }
        public String getReason() { return reason; }
    }
}
