package com.example.application.employee_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Used both to create a brand-new login for an employee that never had one,
 * and to reactivate a previously disabled account. When reactivating an
 * existing User, username/password/roleId are optional - only supply them
 * to change credentials at the same time.
 */
public class EnableLoginRequest {

    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private Long roleId;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
