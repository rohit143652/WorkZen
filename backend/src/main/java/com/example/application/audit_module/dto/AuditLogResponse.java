package com.example.application.audit_module.dto;

import java.time.LocalDateTime;

public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String action;
    private String ipAddress;
    private String userAgent;
    private String description;
    private LocalDateTime createdAt;

    public AuditLogResponse(Long id, Long userId, String action, String ipAddress, String userAgent,
                             String description, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getAction() { return action; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
