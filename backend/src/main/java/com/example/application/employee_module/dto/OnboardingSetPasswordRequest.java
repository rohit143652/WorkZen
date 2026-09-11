package com.example.application.employee_module.dto;

import jakarta.validation.constraints.NotBlank;

public class OnboardingSetPasswordRequest {
    @NotBlank(message = "token is required")
    private String token;

    @NotBlank(message = "verificationCode is required")
    private String verificationCode;

    @NotBlank(message = "password is required")
    private String password;

    @NotBlank(message = "confirmPassword is required")
    private String confirmPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
