package com.example.application.mail_module.dto;

/** password is write-only: null/blank on an UPDATE means "keep the saved password"; it is required the first time. */
public record MailSettingsRequest(String host, Integer port, String username, String fromAddress,
                                  String password, Boolean enabled) {
}
