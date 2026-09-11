package com.example.application.employee_module.dto;

import jakarta.validation.constraints.NotBlank;

public class OnboardingVerifyCodeRequest {
    @NotBlank(message = "token is required")
    private String token;

    @NotBlank(message = "code is required")
    private String code;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
