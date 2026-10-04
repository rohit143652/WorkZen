package com.example.application.employee_module.dto;

/**
 * Returned by EmployeeService.resetPassword() - previously just the plain temporary password,
 * with no way for the admin to know whether the matching notification email to the employee
 * actually sent or silently failed (EmailService.sendHtml() never throws, by design, so a failure
 * there is otherwise invisible unless someone checks the backend logs).
 */
public record PasswordResetResult(String temporaryPassword, boolean emailSent) {
}
