package com.example.application.role_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public class RoleRequest {
    @NotBlank(message = "Role name is required")
    private String name;
    private String description;
    private Set<Long> permissionIds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Set<Long> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(Set<Long> permissionIds) { this.permissionIds = permissionIds; }
}
