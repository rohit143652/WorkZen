package com.example.application.designation_module.dto;

import jakarta.validation.constraints.NotBlank;

public class DesignationRequest {
    @NotBlank(message = "Designation name is required")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
