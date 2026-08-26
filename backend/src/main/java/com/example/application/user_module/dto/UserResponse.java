package com.example.application.user_module.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean active;
    private boolean locked;
    private LocalDateTime lastLoginAt;
    private Set<String> roles;

    public UserResponse(Long id, String username, String email, String firstName, String lastName,
                         boolean active, boolean locked, LocalDateTime lastLoginAt, Set<String> roles) {
        this.id = id; this.username = username; this.email = email;
        this.firstName = firstName; this.lastName = lastName;
        this.active = active; this.locked = locked; this.lastLoginAt = lastLoginAt; this.roles = roles;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public boolean isActive() { return active; }
    public boolean isLocked() { return locked; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public Set<String> getRoles() { return roles; }
}
