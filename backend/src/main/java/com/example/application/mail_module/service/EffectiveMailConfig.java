package com.example.application.mail_module.service;

/**
 * The outgoing-mail settings that will actually be used for one send. {@code source} says where they
 * came from: COMPANY (that client company's own sender), PLATFORM (the platform default a Super
 * Admin saved) or ENVIRONMENT (the MAIL_* variables). {@code problem} is null when usable, otherwise a
 * plain-language reason it can't be. Holds the plain password, so it is only ever passed straight to
 * the mail sender - never logged, never returned from an API.
 */
public record EffectiveMailConfig(String source, String host, int port, String username, String password,
                                  String fromAddress, String problem) {
    public boolean usable() {
        return problem == null;
    }

    public boolean fromEnvironment() {
        return MailSettingsService.SOURCE_ENVIRONMENT.equals(source);
    }
}
