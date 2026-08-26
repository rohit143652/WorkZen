package com.example.application.employee_module.dto;

import jakarta.validation.constraints.NotNull;

public class AssignRoleRequest {
    @NotNull(message = "roleId is required")
    private Long roleId;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
