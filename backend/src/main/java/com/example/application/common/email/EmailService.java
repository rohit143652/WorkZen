package com.example.application.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Thin wrapper around Spring's auto-configured JavaMailSender (see spring.mail.* in
 * application.yml, populated entirely from MAIL_* environment variables - no credential ever
 * appears in code or config files). Currently used only for employee onboarding invitation
 * emails, but written generically enough for any future transactional email need.
 *
 * Never logs the email body (which may contain a verification code) - only that a send was
 * attempted/succeeded/failed, and to which address, matching this codebase's "never log
 * secrets" rule applied elsewhere to passwords/tokens.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${onboarding.mail-from}")
    private String defaultFrom;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Sends an HTML email. Returns false (never throws) on failure - a failed email should not crash whatever business operation triggered it; the caller decides how to surface that. */
    public boolean sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
            return true;
        } catch (MailException | jakarta.mail.MessagingException e) {
            // Deliberately logs only the exception type/message, never the email body (which may
            // contain a one-time verification code) - matches the "never log secrets" rule this
            // codebase already applies to passwords and tokens elsewhere.
            log.error("Failed to send email to {} - {}", to, e.getMessage());
            return false;
        }
    }
}
