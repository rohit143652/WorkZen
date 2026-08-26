package com.example.application.role_module.dto;

import java.util.Set;

public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private Set<String> permissions;
    /** True if this is a tenant-created custom role rather than a global/house system role. */
    private boolean custom;

    public RoleResponse(Long id, String name, String description, boolean active, Set<String> permissions, boolean custom) {
        this.id = id; this.name = name; this.description = description;
        this.active = active; this.permissions = permissions; this.custom = custom;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public Set<String> getPermissions() { return permissions; }
    public boolean isCustom() { return custom; }
}
