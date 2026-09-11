package com.example.application.employee_module.dto;

import jakarta.validation.constraints.Size;

/**
 * Used both to create a brand-new login for an employee that never had one,
 * and to reactivate a previously disabled account. When reactivating an
 * existing User, username/password/roleId are optional - only supply them
 * to change credentials at the same time.
 *
 * password has NO @Size here deliberately - @Size(min=8) would fail validation even for an
 * intentionally-empty string (only @NotBlank/@NotNull skip blank values, @Size still checks
 * length), which breaks the self-onboarding flow where an empty password is exactly what
 * triggers sending an invitation instead of setting one directly. The minimum-length check for
 * an ADMIN-SUPPLIED password happens in EmployeeService itself, only when one is actually given.
 */
public class EnableLoginRequest {

    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    private String password;

    private Long roleId;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
