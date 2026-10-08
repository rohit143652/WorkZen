package com.example.application.mail_module.service;

import com.example.application.common.time.AppTime;
import com.example.application.audit_module.service.AuditService;
import com.example.application.client_company_module.entity.ClientCompany;
import com.example.application.client_company_module.repository.ClientCompanyRepository;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.mail_module.dto.MailSettingsOverview;
import com.example.application.mail_module.dto.MailSettingsOverviewRow;
import com.example.application.mail_module.dto.MailSettingsRequest;
import com.example.application.mail_module.dto.MailSettingsResponse;
import com.example.application.mail_module.entity.MailSettings;
import com.example.application.mail_module.repository.MailSettingsRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Owns the outgoing-email (sender) settings, which a Super Admin manages PER CLIENT COMPANY.
 *
 * Which sender a given email uses - decided on every send, in this order:
 *   1. the client company's OWN saved sender (if it has one and it is enabled)
 *   2. otherwise the PLATFORM DEFAULT saved by a Super Admin (if present and enabled)
 *   3. otherwise the MAIL_* environment variables
 * so a company with no sender of its own still gets its mail out, and a server with nothing saved
 * behaves exactly as it always did.
 *
 * The username and password are stored in the database as entered and read straight from it when
 * an email is sent (no encryption key to manage - a deliberate product decision). The password is
 * never returned by any method here, never written to the audit log, and never logged: callers see
 * only whether one is saved. Every write method is reached only through endpoints that require the
 * Super-Admin-only MAIL_SETTINGS_MANAGE permission.
 */
@Service
public class MailSettingsService {

    public static final String SOURCE_COMPANY = "COMPANY";
    public static final String SOURCE_PLATFORM = "PLATFORM";
    public static final String SOURCE_ENVIRONMENT = "ENVIRONMENT";
    public static final String PLATFORM_NAME = "Platform default";

    /** Passwords saved by the earlier encrypted version start with this - an unreadable blob, not a usable password (see fromRow). */
    static final String LEGACY_ENCRYPTED_PREFIX = "v1:";

    private final MailSettingsRepository repository;
    private final AuditService auditService;
    private final ClientCompanyRepository clientCompanyRepository;

    @Value("${spring.mail.host:}")
    private String envHost;
    @Value("${spring.mail.port:587}")
    private int envPort;
    @Value("${spring.mail.username:}")
    private String envUsername;
    @Value("${spring.mail.password:}")
    private String envPassword;
    @Value("${onboarding.mail-from:}")
    private String envFrom;

    public MailSettingsService(MailSettingsRepository repository, AuditService auditService,
                               ClientCompanyRepository clientCompanyRepository) {
        this.repository = repository;
        this.auditService = auditService;
        this.clientCompanyRepository = clientCompanyRepository;
    }

    // ------------------------------------------------------------------ which sender is used

    /**
     * What an email for this company will really be sent with. {@code null} asks about the platform
     * default itself (company-less: its own row, else the environment). Called on every send, from
     * any thread - needs no tenant or security context.
     */
    @Transactional(readOnly = true)
    public EffectiveMailConfig resolveEffective(Long clientCompanyId) {
        MailSettings own = clientCompanyId == null ? null : repository.findByClientCompanyId(clientCompanyId).orElse(null);
        MailSettings platform = repository.findFirstByClientCompanyIdIsNullOrderByIdAsc().orElse(null);
        return resolveFrom(own, platform);
    }

    private EffectiveMailConfig resolveFrom(MailSettings own, MailSettings platform) {
        if (own != null && own.isEnabled()) {
            return fromRow(SOURCE_COMPANY, own);
        }
        if (platform != null && platform.isEnabled()) {
            return fromRow(SOURCE_PLATFORM, platform);
        }
        return fromEnvironment();
    }

    private EffectiveMailConfig fromRow(String source, MailSettings m) {
        String password = m.getPassword();
        if (password != null && password.startsWith(LEGACY_ENCRYPTED_PREFIX)) {
            // Written by the earlier version that encrypted passwords with a key that no longer exists.
            // Sending this as a password would just fail confusingly at the mail server, so say so plainly.
            return new EffectiveMailConfig(source, m.getHost(), m.getPort(), m.getUsername(), null, m.getFromAddress(),
                    "The saved password is in an old encrypted format that can no longer be read. Re-enter the password in Email Settings.");
        }
        return new EffectiveMailConfig(source, m.getHost(), m.getPort(), m.getUsername(), password, m.getFromAddress(), null);
    }

    private EffectiveMailConfig fromEnvironment() {
        List<String> missing = new ArrayList<>();
        if (isBlank(envUsername)) missing.add("MAIL_USERNAME");
        if (isBlank(envPassword)) missing.add("MAIL_PASSWORD");
        if (isBlank(envFrom)) missing.add("MAIL_FROM");
        String problem = missing.isEmpty() ? null
                : "No sender is set up for this: no saved sender in Email Settings, and these are EMPTY in the running backend: "
                + String.join(", ", missing) + ". Save a sender in Super Admin -> Email Settings, or set them as real environment "
                + "variables in the same terminal the backend is started from (a .env file is not read automatically).";
        return new EffectiveMailConfig(SOURCE_ENVIRONMENT, envHost, envPort, envUsername, envPassword, envFrom, problem);
    }

    // ------------------------------------------------------------------ reading (never returns a password)

    /** Display name for a scope; null = the platform default. A company id that doesn't exist is a 404, not a silent platform fallback. */
    @Transactional(readOnly = true)
    public String scopeName(Long clientCompanyId) {
        if (clientCompanyId == null) {
            return PLATFORM_NAME;
        }
        return clientCompanyRepository.findById(clientCompanyId).map(ClientCompany::getCompanyName)
                .orElseThrow(() -> new ResourceNotFoundException("Client company not found: " + clientCompanyId));
    }

    private Optional<MailSettings> rowFor(Long clientCompanyId) {
        return clientCompanyId == null
                ? repository.findFirstByClientCompanyIdIsNullOrderByIdAsc()
                : repository.findByClientCompanyId(clientCompanyId);
    }

    @Transactional(readOnly = true)
    public MailSettingsResponse view(Long clientCompanyId) {
        String name = scopeName(clientCompanyId);
        Optional<MailSettings> row = rowFor(clientCompanyId);
        EffectiveMailConfig eff = resolveEffective(clientCompanyId);
        return new MailSettingsResponse(
                clientCompanyId, name,
                row.isPresent(),
                row.map(MailSettings::getHost).orElse(null),
                row.map(MailSettings::getPort).orElse(null),
                row.map(MailSettings::getUsername).orElse(null),
                row.map(MailSettings::getFromAddress).orElse(null),
                row.map(MailSettings::isEnabled).orElse(null),
                row.isPresent(),                       // a saved row always has a password
                row.map(MailSettings::getUpdatedAt).orElse(null),
                eff.source(), eff.host(), eff.port(), eff.username(), eff.fromAddress(),
                eff.usable(), eff.problem());
    }

    /** The overview table: the platform default plus every client company and who it sends as. One query for all saved rows, resolved in memory. */
    @Transactional(readOnly = true)
    public MailSettingsOverview overview() {
        List<MailSettings> rows = repository.findAll();
        MailSettings platform = rows.stream().filter(r -> r.getClientCompanyId() == null).findFirst().orElse(null);
        Map<Long, MailSettings> byCompany = rows.stream().filter(r -> r.getClientCompanyId() != null)
                .collect(Collectors.toMap(MailSettings::getClientCompanyId, r -> r, (a, b) -> a));

        List<MailSettingsOverviewRow> companies = clientCompanyRepository.findAll().stream()
                .sorted(Comparator.comparing(ClientCompany::getCompanyName, String.CASE_INSENSITIVE_ORDER))
                .map(c -> {
                    MailSettings own = byCompany.get(c.getId());
                    EffectiveMailConfig eff = resolveFrom(own, platform);
                    return new MailSettingsOverviewRow(c.getId(), c.getCompanyName(), own != null,
                            own != null && own.isEnabled(), eff.source(), eff.fromAddress(), eff.usable(), eff.problem());
                })
                .toList();

        EffectiveMailConfig platformEff = resolveFrom(null, platform);
        MailSettingsOverviewRow platformRow = new MailSettingsOverviewRow(null, PLATFORM_NAME, platform != null,
                platform != null && platform.isEnabled(), platformEff.source(), platformEff.fromAddress(),
                platformEff.usable(), platformEff.problem());
        return new MailSettingsOverview(platformRow, companies);
    }

    // ------------------------------------------------------------------ writing

    @Transactional
    public MailSettingsResponse save(Long clientCompanyId, MailSettingsRequest request, Long actorId, HttpServletRequest httpRequest) {
        String scopeName = scopeName(clientCompanyId);   // 404 for an unknown company
        String host = requireText(request.host(), "Mail server host");
        String username = requireText(request.username(), "Username");
        String fromAddress = requireText(request.fromAddress(), "From address");
        if (request.port() == null || request.port() < 1 || request.port() > 65535) {
            throw new BadRequestException("Port must be a number between 1 and 65535 (587 for STARTTLS, 465 for SSL).");
        }
        try {
            new InternetAddress(fromAddress, true).validate();
        } catch (AddressException e) {
            throw new BadRequestException("From address is not a valid email address: " + fromAddress);
        }

        Optional<MailSettings> existing = rowFor(clientCompanyId);
        String newPassword = normalizePassword(host, request.password());
        if (existing.isEmpty() && newPassword == null) {
            throw new BadRequestException("A password is required when saving email settings for the first time.");
        }

        MailSettings settings = existing.orElseGet(MailSettings::new);
        settings.setClientCompanyId(clientCompanyId);
        settings.setHost(host);
        settings.setPort(request.port());
        settings.setUsername(username);
        settings.setFromAddress(fromAddress);
        settings.setEnabled(request.enabled() == null || request.enabled());
        if (newPassword != null) {
            settings.setPassword(newPassword);
        }
        settings.setUpdatedBy(actorId);
        settings.setUpdatedAt(AppTime.now());
        repository.save(settings);

        // Describes what changed - and deliberately never contains the password.
        auditService.log(actorId, "MAIL_SETTINGS_UPDATED",
                "Email sender saved for " + scopeName + ": server " + host + ":" + request.port() + ", login " + username
                        + ", sender " + fromAddress + ", " + (settings.isEnabled() ? "enabled" : "disabled")
                        + (newPassword != null ? ", password changed" : ", password unchanged"),
                httpRequest);
        return view(clientCompanyId);
    }

    @Transactional
    public MailSettingsResponse delete(Long clientCompanyId, Long actorId, HttpServletRequest httpRequest) {
        String scopeName = scopeName(clientCompanyId);
        rowFor(clientCompanyId).ifPresent(row -> {
            repository.delete(row);
            auditService.log(actorId, "MAIL_SETTINGS_DELETED",
                    "Saved email sender deleted for " + scopeName + " (login " + row.getUsername() + ")"
                            + (clientCompanyId == null ? " - falling back to the MAIL_* environment variables"
                            : " - this company now uses the platform default sender"),
                    httpRequest);
        });
        return view(clientCompanyId);
    }

    /**
     * Google shows an App Password as four groups of four letters separated by spaces purely for
     * readability - the real password has no spaces, and pasting it with them is a classic cause of
     * "Username and Password not accepted". For Gmail every whitespace character is removed; for any
     * other provider only the ends are trimmed, since their passwords may legitimately contain spaces.
     */
    static String normalizePassword(String host, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return host.toLowerCase().endsWith("gmail.com") ? raw.replaceAll("\\s+", "") : raw.trim();
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(label + " is required.");
        }
        return value.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
