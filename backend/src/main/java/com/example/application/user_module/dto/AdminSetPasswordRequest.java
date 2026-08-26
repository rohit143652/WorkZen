package com.example.application.user_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * For an admin (e.g. CLIENT_ADMIN) choosing a specific new password for a
 * user, as distinct from resetPassword's randomly-generated one - e.g. an
 * admin who has verified the employee's identity over the phone and wants
 * to set a password the employee already knows to expect.
 */
public class AdminSetPasswordRequest {
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm the new password")
    private String confirmPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
