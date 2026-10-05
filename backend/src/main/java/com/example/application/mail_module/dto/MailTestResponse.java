package com.example.application.mail_module.dto;

/** The HTTP call itself succeeded either way - {@code sent} says whether the mail server accepted the message, {@code message} says why not when it didn't. */
public record MailTestResponse(boolean sent, String message) {
}
