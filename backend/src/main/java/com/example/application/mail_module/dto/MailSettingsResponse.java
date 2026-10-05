package com.example.application.mail_module.dto;

import java.time.LocalDateTime;

/**
 * What the Email Settings editor shows for ONE scope - a client company, or the platform default
 * ({@code clientCompanyId} null). The password is NEVER in here - only whether one is saved
 * ({@code passwordSet}). The first block describes the row saved for exactly this scope (null when
 * none); the "effective" block is what emails for this scope are ACTUALLY sent with right now - its
 * own row when it has one, else the platform default, else the MAIL_* environment variables.
 */
public record MailSettingsResponse(
        Long clientCompanyId, String scopeName,
        boolean hasStoredSettings,
        String host, Integer port, String username, String fromAddress, Boolean enabled,
        boolean passwordSet, LocalDateTime updatedAt,
        String effectiveSource, String effectiveHost, Integer effectivePort,
        String effectiveUsername, String effectiveFromAddress,
        boolean usable, String problem) {
}
