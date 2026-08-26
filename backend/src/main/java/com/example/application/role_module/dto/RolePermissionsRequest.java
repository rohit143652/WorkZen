package com.example.application.role_module.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class RolePermissionsRequest {
    @NotNull(message = "permissionIds is required")
    private Set<Long> permissionIds;

    public Set<Long> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(Set<Long> permissionIds) { this.permissionIds = permissionIds; }
}
