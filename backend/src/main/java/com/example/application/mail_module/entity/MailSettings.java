package com.example.application.mail_module.entity;

import com.example.application.common.time.AppTime;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * An outgoing-email (SMTP) account, managed by a Super Admin. {@code clientCompanyId} says whose it
 * is: a client company's own sender, or NULL for the platform default that companies without
 * their own fall back to (see V122).
 *
 * The password is stored as entered (plain text) - a deliberate product decision: no encryption key
 * to manage. Protect database access and backups accordingly, and prefer a revocable app-specific
 * password over a mailbox's main one. It is never returned by any API or written to any log.
 */
@Entity
@Table(name = "mail_settings")
public class MailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The client company this sender belongs to; null = the platform default. */
    @Column(name = "client_company_id")
    private Long clientCompanyId;

    @Column(nullable = false, length = 255)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false, length = 255)
    private String username;

    @Column(nullable = false, length = 1024)
    private String password;

    @Column(name = "from_address", nullable = false, length = 255)
    private String fromAddress;

    /** Disabled = keep the saved row but ignore it, falling back to the server's MAIL_* environment variables. */
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = AppTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = AppTime.now();

    public Long getId() { return id; }
    public Long getClientCompanyId() { return clientCompanyId; }
    public void setClientCompanyId(Long clientCompanyId) { this.clientCompanyId = clientCompanyId; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
