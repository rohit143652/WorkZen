package com.example.application.employee_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Nested "Login Access" section of the employee form. Only required/validated
 * when the employee's enableLogin flag is true - see EmployeeRequest.
 */
public class EmployeeLoginAccessRequest {

    @NotBlank(message = "Username is required when login is enabled")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    /** Optional as of the employee self-onboarding feature - if omitted, the employee is sent a
        secure invitation to set their OWN password instead of the admin setting one directly
        (see EmployeeOnboardingService). Still supported if provided, for any workflow that
        genuinely needs an admin-set password immediately (existing behavior, unchanged).
        Deliberately NO @Size here - @Size(min=8) fails even for an intentionally-empty string
        (only @NotBlank/@NotNull skip blank values), which would break the invitation flow this
        field's absence is supposed to trigger. Length is checked in EmployeeService itself, only
        when a password is actually supplied. */
    private String password;

    private String confirmPassword;

    @NotNull(message = "Role is required when login is enabled")
    private Long roleId;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
