package com.example.application.employee_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.client_company_module.entity.ClientCompany;
import com.example.application.client_company_module.repository.ClientCompanyRepository;
import com.example.application.common.email.EmailService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.employee_module.dto.OnboardingSetPasswordRequest;
import com.example.application.employee_module.dto.OnboardingValidateResponse;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.entity.EmployeeOnboardingInvitation;
import com.example.application.employee_module.repository.EmployeeOnboardingInvitationRepository;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * The complete employee self-onboarding lifecycle: invite -> verify one-time code -> set own
 * password -> account activated. No suitable existing token/verification mechanism was found in
 * this codebase to extend (only RefreshToken exists, which is unrelated JWT refresh machinery) -
 * this is a genuinely new, self-contained flow.
 *
 * Security design:
 *   - The invitation TOKEN (goes in the email link) is looked up by a SHA-256 hash - a fast,
 *     DETERMINISTIC hash is required here since the lookup is "find the row whose hash equals
 *     this", which a salted/slow hash (bcrypt) cannot do. This is safe because the token itself
 *     has high entropy (32 random bytes) - the same reasoning session/API-key tokens commonly use.
 *   - The verification CODE (6 digits, low entropy) is hashed with the EXISTING PasswordEncoder
 *     (bcrypt, the same one used for real passwords) and checked via passwordEncoder.matches()
 *     against the one specific invitation row already found by token - no WHERE-by-code-hash
 *     lookup is ever needed, so bcrypt's slow/salted nature is not a problem here, and reusing it
 *     avoids introducing a second hashing scheme into the codebase.
 *   - Neither the plain token nor the plain code is EVER persisted - only their hashes.
 */
@Service
public class EmployeeOnboardingService {

    private final EmployeeOnboardingInvitationRepository invitationRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ClientCompanyRepository clientCompanyRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;
    private final com.example.application.common.tenant.TenantContextService tenantContext;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${onboarding.frontend-base-url}")
    private String frontendBaseUrl;

    @Value("${onboarding.invitation-expiry-hours}")
    private long invitationExpiryHours;

    @Value("${onboarding.max-code-attempts}")
    private int maxCodeAttempts;

    @Value("${app.password-policy.min-length:8}")
    private int passwordMinLength;

    public EmployeeOnboardingService(EmployeeOnboardingInvitationRepository invitationRepository,
                                      EmployeeRepository employeeRepository, UserRepository userRepository,
                                      ClientCompanyRepository clientCompanyRepository, PasswordEncoder passwordEncoder,
                                      EmailService emailService, AuditService auditService,
                                      com.example.application.common.tenant.TenantContextService tenantContext) {
        this.invitationRepository = invitationRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.clientCompanyRepository = clientCompanyRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditService = auditService;
        this.tenantContext = tenantContext;
    }

    /**
     * Creates (and emails) a fresh invitation for this employee/user pair - called both from
     * EmployeeService.create() (Login Required = YES at creation time) and from the separate
     * "Enable Login" / "Resend Invitation" actions on an existing employee. Any previously
     * PENDING invitation for this employee is superseded first, so there is never more than one
     * simultaneously valid invitation - resending genuinely invalidates the old one rather than
     * leaving two working links.
     */
    @Transactional
    public void createInvitation(Employee employee, User user, Long actorId, HttpServletRequest httpRequest) {
        invitationRepository.findAllByEmployeeIdAndStatus(employee.getId(), "PENDING")
                .forEach(existing -> {
                    existing.setStatus("SUPERSEDED");
                    invitationRepository.save(existing);
                });

        String token = generateToken();
        String code = generateVerificationCode();

        EmployeeOnboardingInvitation invitation = new EmployeeOnboardingInvitation();
        invitation.setClientCompanyId(employee.getClientCompanyId());
        invitation.setEmployeeId(employee.getId());
        invitation.setUserId(user.getId());
        invitation.setTokenHash(sha256(token));
        invitation.setVerificationCodeHash(passwordEncoder.encode(code));
        invitation.setExpiresAt(LocalDateTime.now().plusHours(invitationExpiryHours));
        invitation.setCreatedBy(actorId);
        invitationRepository.save(invitation);

        employee.setOnboardingStatus("INVITED");
        employeeRepository.save(employee);

        sendInvitationEmail(employee, token, code);

        auditService.log(actorId, "ONBOARDING_INVITATION_SENT",
                "Sent onboarding invitation to employee " + employee.getEmployeeCode(), httpRequest);
    }

    /** Admin action - invalidates any existing invitation and sends a completely new one. Reuses createInvitation()'s own supersede logic, so there is never a stale still-working link left behind. */
    @Transactional
    public void resendInvitation(Long employeeId, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = employeeRepository.findByIdAndClientCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        if (!employee.hasLogin()) {
            throw new BadRequestException("This employee does not have a login account - use Enable Login first.");
        }
        createInvitation(employee, employee.getUser(), actorId, httpRequest);
        invitationRepository.findFirstByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .ifPresent(inv -> { inv.setResentAt(LocalDateTime.now()); invitationRepository.save(inv); });
    }

    /** Public (unauthenticated) - the onboarding landing page's first call. Exposes only what's needed to greet the employee safely: their name, the company name, and whether the link is already expired/used. Never exposes the employee id, user id, or anything else. */
    @Transactional(readOnly = true)
    public OnboardingValidateResponse validateToken(String token) {
        EmployeeOnboardingInvitation invitation = findInvitationByToken(token);
        Employee employee = employeeRepository.findById(invitation.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation is no longer valid."));
        ClientCompany company = clientCompanyRepository.findById(invitation.getClientCompanyId()).orElse(null);

        OnboardingValidateResponse response = new OnboardingValidateResponse();
        response.setEmployeeName((employee.getFirstName() + " " + employee.getLastName()).trim());
        response.setEmployeeCode(employee.getEmployeeCode());
        response.setCompanyName(company != null ? company.getCompanyName() : "");
        response.setExpired(invitation.isExpired());
        response.setAlreadyUsed("USED".equals(invitation.getStatus()));
        return response;
    }

    /** Public - checks the one-time code without yet setting a password (lets the frontend show "code verified, now choose a password" as a distinct step, matching the spec's step-by-step flow). */
    @Transactional
    public void verifyCode(String token, String code) {
        EmployeeOnboardingInvitation invitation = requireUsableInvitation(token);
        checkCode(invitation, code);
        invitation.setStatus("VERIFIED");
        invitationRepository.save(invitation);
    }

    /** Public - the final step: verify the code (again, safely - never trust that the frontend actually called verifyCode() first) and set the employee's own password. */
    @Transactional
    public void setPassword(OnboardingSetPasswordRequest request) {
        EmployeeOnboardingInvitation invitation = requireUsableInvitation(request.getToken());
        checkCode(invitation, request.getVerificationCode());

        if (request.getPassword() == null || request.getPassword().length() < passwordMinLength) {
            throw new BadRequestException("Password must be at least " + passwordMinLength + " characters");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }

        User user = userRepository.findById(invitation.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation is no longer valid."));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        user.setActive(true);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        invitation.setStatus("USED");
        invitation.setUsedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        Employee employee = employeeRepository.findById(invitation.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation is no longer valid."));
        employee.setOnboardingStatus("PROFILE_IN_PROGRESS");
        employeeRepository.save(employee);

        // No HttpServletRequest available here (this is a public, unauthenticated endpoint) -
        // AuditService gracefully handles a null request (see its own null-check), so this is safe.
        auditService.log(null, "ONBOARDING_PASSWORD_SET",
                "Employee " + employee.getEmployeeCode() + " completed onboarding password setup", null);
    }

    /** Shared by verifyCode() and setPassword() - expiry + status checks that must happen every time a code is checked, not just once. */
    private EmployeeOnboardingInvitation requireUsableInvitation(String token) {
        EmployeeOnboardingInvitation invitation = findInvitationByToken(token);
        if (invitation.isExpired()) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new BadRequestException("This invitation has expired. Please ask your admin to resend it.");
        }
        if ("LOCKED".equals(invitation.getStatus())) {
            throw new BadRequestException("Too many incorrect attempts. Please ask your admin to resend the invitation.");
        }
        if ("USED".equals(invitation.getStatus())) {
            throw new BadRequestException("This invitation has already been used.");
        }
        return invitation;
    }

    private void checkCode(EmployeeOnboardingInvitation invitation, String code) {
        if (invitation.getAttemptCount() >= maxCodeAttempts) {
            invitation.setStatus("LOCKED");
            invitationRepository.save(invitation);
            throw new BadRequestException("Too many incorrect attempts. Please ask your admin to resend the invitation.");
        }
        if (code == null || !passwordEncoder.matches(code, invitation.getVerificationCodeHash())) {
            invitation.setAttemptCount(invitation.getAttemptCount() + 1);
            invitationRepository.save(invitation);
            // Deliberately vague - never reveals whether the token itself was fine and only the
            // code was wrong, nor how many attempts remain, to avoid helping a brute-force attempt.
            throw new BadRequestException("Incorrect verification code.");
        }
    }

    /** Finds the CURRENTLY ACTIVE invitation for a token - PENDING or VERIFIED only. A token whose row is SUPERSEDED/USED/EXPIRED/LOCKED must never be treated as valid again just because its hash still matches something in the table (resending never deletes the old row, see class javadoc). */
    private EmployeeOnboardingInvitation findInvitationByToken(String token) {
        String hash = sha256(token);
        return invitationRepository.findAllByTokenHash(hash).stream()
                .filter(i -> "PENDING".equals(i.getStatus()) || "VERIFIED".equals(i.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("This invitation link is invalid or no longer active."));
    }

    private void sendInvitationEmail(Employee employee, String token, String code) {
        ClientCompany company = clientCompanyRepository.findById(employee.getClientCompanyId()).orElse(null);
        String companyName = company != null ? company.getCompanyName() : "your company";
        String link = frontendBaseUrl + "/employee-onboarding/" + token;
        String employeeName = (employee.getFirstName() + " " + employee.getLastName()).trim();

        String html = "<div style=\"font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;\">"
                + "<h2>Welcome to " + escapeHtml(companyName) + "</h2>"
                + "<p>Hello " + escapeHtml(employeeName) + ",</p>"
                + "<p>Your employee account has been created.</p>"
                + "<p><strong>Employee Number:</strong> " + escapeHtml(employee.getEmployeeCode()) + "</p>"
                + "<p>Click the secure link below to activate your account and complete your profile:</p>"
                + "<p><a href=\"" + link + "\" style=\"display:inline-block;padding:10px 20px;background:#2563eb;color:#fff;"
                + "text-decoration:none;border-radius:6px;\">Activate My Account</a></p>"
                + "<p>You'll be asked to enter this one-time verification code before setting your password:</p>"
                + "<p style=\"font-size: 28px; font-weight: bold; letter-spacing: 4px;\">" + code + "</p>"
                + "<p style=\"color:#666; font-size: 13px;\">This code and link expire in " + invitationExpiryHours + " hours.</p>"
                + "<p style=\"color:#666; font-size: 13px;\">If you did not expect this email, you can safely ignore it.</p>"
                + "</div>";

        emailService.sendHtml(employee.getEmail(), "Activate your account - " + companyName, html);
    }

    private static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 32 random bytes, URL-safe Base64 - high entropy, never predictable/sequential (spec explicitly forbids employee-id-based or timestamp-only tokens). */
    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 6-digit numeric code, zero-padded - low entropy by design (a human types this in), which is exactly why it's rate-limited via attemptCount/maxCodeAttempts rather than relied on for its own entropy. */
    private String generateVerificationCode() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
