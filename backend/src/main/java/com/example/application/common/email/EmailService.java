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

    // Read only to detect "mail was never configured" - never logged or exposed anywhere.
    @Value("${spring.mail.username:}")
    private String mailUsername;
    @Value("${spring.mail.password:}")
    private String mailPassword;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Surfaces the single most common cause of "invitation email could NOT be sent" the moment the
     * app starts, instead of only after an admin has already created an employee and been told it
     * failed: MAIL_USERNAME / MAIL_PASSWORD were never set (they deliberately have no default - see
     * application.yml - and environment variables set in one PowerShell window don't carry over to
     * a new one). Only the fact that they're blank is logged, never their values.
     */
    @jakarta.annotation.PostConstruct
    void warnIfMailNotConfigured() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (isBlank(mailUsername)) missing.add("MAIL_USERNAME");
        if (isBlank(mailPassword)) missing.add("MAIL_PASSWORD");
        if (isBlank(defaultFrom)) missing.add("MAIL_FROM");
        if (missing.isEmpty()) {
            log.info("Email configuration found. SMTP login account (MAIL_USERNAME): [{}] | Sender shown to the employee (MAIL_FROM): [{}]",
                    mailUsername.trim(), defaultFrom.trim());
            if (!mailUsername.trim().equalsIgnoreCase(defaultFrom.trim())) {
                // A stale MAIL_FROM left over in the terminal after MAIL_USERNAME was changed is an easy
                // mistake to make, and it's invisible unless both are shown side by side.
                log.warn("MAIL_USERNAME and MAIL_FROM are DIFFERENT addresses. Gmail only accepts a different sender "
                        + "if it is a verified 'Send mail as' alias of the login account - otherwise it replaces or "
                        + "rejects it. Normally set both to the SAME address, and create the App Password from THAT account.");
            }
            return;
        }
        // Names only - never values. Spring Boot reads real OS environment variables, NOT a .env
        // file, so a value written only in a file never reaches this JVM.
        log.warn("Email is NOT configured. These are EMPTY in the running backend: {}. Every invitation / "
                + "password-reset email will fail until they are set as real environment variables in the "
                + "SAME terminal window the backend is started from (a .env file is not read automatically).",
                String.join(", ", missing));
    }

    /** Sends an HTML email. Returns false (never throws) on failure - a failed email should not crash whatever business operation triggered it; the caller decides how to surface that. */
    public boolean sendHtml(String to, String subject, String htmlBody) {
        // Checked one at a time BEFORE touching the mail library, because the library's own
        // "Illegal address" message never says WHICH address it rejected - the sender (MAIL_FROM)
        // or the recipient - and those have completely different fixes. Also trims stray
        // whitespace, a common copy/paste artifact in an environment variable's value.
        String from = defaultFrom == null ? null : defaultFrom.trim();
        String recipient = to == null ? null : to.trim();
        String addressProblem = describeAddressProblem("SENDER (MAIL_FROM / MAIL_USERNAME)", from);
        if (addressProblem == null) addressProblem = describeAddressProblem("RECIPIENT (the employee's email)", recipient);
        if (addressProblem != null) {
            log.error("Email NOT sent - {}", addressProblem);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
            return true;
        } catch (MailException | jakarta.mail.MessagingException e) {
            // Deliberately logs only the exception type/message, never the email body (which may
            // contain a one-time verification code) - matches the "never log secrets" rule this
            // codebase already applies to passwords and tokens elsewhere.
            String hint = "";
            if (isBlank(defaultFrom)) {
                // "Illegal address" in this situation refers to the SENDER, not the recipient -
                // an empty From address is rejected by the mail library with exactly that message.
                hint = " | LIKELY CAUSE: MAIL_FROM (or MAIL_USERNAME) is empty - 'Illegal address' means the "
                        + "SENDER address is blank, not the recipient's";
            } else if (isBlank(mailUsername) || isBlank(mailPassword)) {
                hint = " | LIKELY CAUSE: MAIL_USERNAME/MAIL_PASSWORD are not set";
            }
            log.error("Failed to send email to {} - {} | root cause: {}{}", to, e.getMessage(), rootCause(e), hint);
            return false;
        }
    }

    /** Null if the address is a usable email; otherwise a precise sentence naming which address is bad and what it actually contains (an email address is not a secret - it appears in every email's headers). */
    private static String describeAddressProblem(String label, String address) {
        if (address == null || address.isBlank()) {
            return label + " address is EMPTY. Set it as a real environment variable in the same terminal the backend starts from.";
        }
        try {
            new jakarta.mail.internet.InternetAddress(address, true).validate();
            return null;
        } catch (jakarta.mail.internet.AddressException e) {
            return label + " address is INVALID: [" + address + "] (length " + address.length() + ") - " + e.getMessage()
                    + ". It must look like name@domain.com, with no quotes, spaces or angle brackets.";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** The innermost exception - e.g. "AuthenticationFailedException: 535 Username and Password not accepted" - which Spring's own MailException wrapper message often hides behind a generic "Mail server connection failed". */
    private static String rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
