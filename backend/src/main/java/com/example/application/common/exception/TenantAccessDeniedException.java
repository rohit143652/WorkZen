package com.example.application.common.exception;

/**
 * Thrown when a tenant-scoped user attempts to act on a resource belonging
 * to a different tenant. Always mapped to a generic "not found"-style
 * response - see GlobalExceptionHandler - to avoid confirming that the
 * other tenant's resource exists at all.
 */
public class TenantAccessDeniedException extends RuntimeException {
    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
