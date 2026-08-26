package com.example.application.user_module.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public class UserRolesRequest {
    @NotEmpty(message = "At least one role must be assigned")
    private Set<Long> roleIds;

    public Set<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(Set<Long> roleIds) { this.roleIds = roleIds; }
}
