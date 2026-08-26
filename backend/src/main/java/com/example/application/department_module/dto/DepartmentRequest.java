package com.example.application.department_module.dto;

import jakarta.validation.constraints.NotBlank;

public class DepartmentRequest {
    @NotBlank(message = "Department name is required")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
