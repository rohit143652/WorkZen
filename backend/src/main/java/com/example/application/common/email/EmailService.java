package com.example.application.common.email;

import com.example.application.mail_module.service.EffectiveMailConfig;
import com.example.application.mail_module.service.MailSettingsService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Sends transactional email. WHICH account it sends with is decided on every send by
 * MailSettingsService, per client company: that company's own sender if a Super Admin saved one,
 * else the platform default sender, else the MAIL_* environment variables (via Spring's
 * auto-configured JavaMailSender). A server with nothing saved behaves exactly as before, and a
 * change made in the app applies to the very next email without a restart.
 *
 * Every send names the company it is for. There is deliberately NO overload without one: a caller
 * that forgot would otherwise send one company's mail from another's address without any error.
 *
 * Never logs the email body (which may contain a verification code or a temporary password) or
 * the SMTP password - only that a send was attempted/succeeded/failed, to which address, how long it took,
 * and why it failed.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /** Auto-configured from the MAIL_* environment variables (spring.mail.*) - used when no saved settings apply. */
    private final JavaMailSender environmentSender;
    private final MailSettingsService mailSettings;

    @Value("${MAIL_CONNECT_TIMEOUT_MS:5000}")
    private int connectTimeoutMs;
    @Value("${MAIL_READ_TIMEOUT_MS:10000}")
    private int readTimeoutMs;
    @Value("${MAIL_WRITE_TIMEOUT_MS:10000}")
    private int writeTimeoutMs;

    public EmailService(JavaMailSender environmentSender, MailSettingsService mailSettings) {
        this.environmentSender = environmentSender;
        this.mailSettings = mailSettings;
    }

    /**
     * Reports at startup which account will be used and where it came from - so a wrong or missing
     * configuration is visible the moment the app starts, not only after an admin has saved an
     * employee and been told the invitation failed. Only addresses are logged, never the password.
     */
    @PostConstruct
    void logConfiguration() {
        try {
            EffectiveMailConfig cfg = mailSettings.resolveEffective(null);   // platform default / environment; companies may override
            if (!cfg.usable()) {
                log.warn("Email is NOT usable (settings source: {}). {}", cfg.source(), cfg.problem());
                return;
            }
            log.info("Email configuration found for the platform default (source: {}). Server: {}:{} | login account: [{}] | sender shown to recipients: [{}]",
                    cfg.source(), cfg.host(), cfg.port(), cfg.username().trim(), cfg.fromAddress().trim());
            if (!cfg.username().trim().equalsIgnoreCase(cfg.fromAddress().trim())) {
                log.warn("The login account and the sender address are DIFFERENT. Gmail only accepts a different sender if it is a "
                        + "verified 'Send mail as' alias of the login account - otherwise it replaces or rejects it. Normally use the SAME address for both.");
            }
        } catch (RuntimeException e) {
            // e.g. the database isn't reachable yet - the settings are re-read on every send anyway.
            log.warn("Could not read the email settings at startup ({}). They will be read again when an email is sent.", e.getMessage());
        }
    }

    /** Boolean form for callers that only need to know whether it went out. */
    public boolean sendHtml(Long clientCompanyId, String to, String subject, String htmlBody) {
        return send(clientCompanyId, to, subject, htmlBody).sent();
    }

    /**
     * Sends an HTML email FROM the sender configured for {@code clientCompanyId} (null = the
     * platform default itself) and says WHY if it didn't work. Never throws for a delivery problem.
     */
    public SendResult send(Long clientCompanyId, String to, String subject, String htmlBody) {
        EffectiveMailConfig cfg = mailSettings.resolveEffective(clientCompanyId);
        if (!cfg.usable()) {
            log.error("Email NOT sent for company {} - {} (settings source: {})", clientCompanyId, cfg.problem(), cfg.source());
            return SendResult.failed(cfg.problem());
        }

        // Checked one at a time BEFORE touching the mail library, because the library's own
        // "Illegal address" message never says WHICH address it rejected - the sender or the
        // recipient - and those have completely different fixes. Also trims stray whitespace.
        String from = cfg.fromAddress().trim();
        String recipient = to == null ? null : to.trim();
        String addressProblem = describeAddressProblem("SENDER (the From address)", from);
        if (addressProblem == null) addressProblem = describeAddressProblem("RECIPIENT (the employee's email)", recipient);
        if (addressProblem != null) {
            log.error("Email NOT sent - {}", addressProblem);
            return SendResult.failed(addressProblem);
        }

        JavaMailSender sender = cfg.fromEnvironment() ? environmentSender : buildSender(cfg);
        // Timed so the real cost of an email on THIS server shows up in the log.
        long startedAt = System.nanoTime();
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            sender.send(message);
            log.info("Email sent successfully to {} in {} ms (company {}, via {} sender {})", to, (System.nanoTime() - startedAt) / 1_000_000,
                    clientCompanyId, cfg.source(), from);
            return SendResult.ok();
        } catch (MailException | jakarta.mail.MessagingException e) {
            String reason = friendlyReason(e, cfg);
            // Logs the exception's own message, never the email body or the password.
            log.error("Failed to send email to {} after {} ms (company {}, via {} sender {}) - {} | root cause: {}", to,
                    (System.nanoTime() - startedAt) / 1_000_000, clientCompanyId, cfg.source(), from, reason, rootCause(e));
            return SendResult.failed(reason);
        }
    }

    /** A sender for a saved (database) configuration. Same options the environment-based one gets from application.yml. */
    private JavaMailSender buildSender(EffectiveMailConfig cfg) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.host());
        sender.setPort(cfg.port());
        sender.setUsername(cfg.username());
        sender.setPassword(cfg.password());
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        if (cfg.port() == 465) {
            props.put("mail.smtp.ssl.enable", "true");          // implicit SSL
        } else {
            props.put("mail.smtp.starttls.enable", "true");     // 587 and most others
        }
        // JavaMail's defaults are "wait forever" - bounded so a dead server can't hang a request or the mail thread.
        props.put("mail.smtp.connectiontimeout", String.valueOf(connectTimeoutMs));
        props.put("mail.smtp.timeout", String.valueOf(readTimeoutMs));
        props.put("mail.smtp.writetimeout", String.valueOf(writeTimeoutMs));
        return sender;
    }

    /** Turns the library's technical exception into a sentence a Super Admin can act on. */
    private static String friendlyReason(Throwable e, EffectiveMailConfig cfg) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof jakarta.mail.AuthenticationFailedException) {
                return "The mail server rejected the username/password. For Gmail use a 16-letter App Password from the SAME account "
                        + "(2-Step Verification must be on), and make sure the username is that account's full address.";
            }
            if (t instanceof java.net.UnknownHostException) {
                return "Cannot find the mail server '" + cfg.host() + "' - check the host name.";
            }
            if (t instanceof java.net.ConnectException || t instanceof java.net.SocketTimeoutException
                    || t instanceof java.net.NoRouteToHostException) {
                return "Cannot connect to " + cfg.host() + ":" + cfg.port() + " - wrong port, a firewall blocking it, or the server is down.";
            }
        }
        return rootCause(e);
    }

    /** Null if the address is usable; otherwise a sentence naming WHICH address is bad and what it contains (an email address is not a secret - it is in every email's headers). */
    private static String describeAddressProblem(String label, String address) {
        if (address == null || address.isBlank()) {
            return label + " address is EMPTY.";
        }
        try {
            new InternetAddress(address, true).validate();
            return null;
        } catch (AddressException e) {
            return label + " address is INVALID: [" + address + "] (length " + address.length() + ") - " + e.getMessage()
                    + ". It must look like name@domain.com, with no quotes, spaces or angle brackets.";
        }
    }

    /** The innermost exception - e.g. "AuthenticationFailedException: 535 Username and Password not accepted". */
    private static String rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
