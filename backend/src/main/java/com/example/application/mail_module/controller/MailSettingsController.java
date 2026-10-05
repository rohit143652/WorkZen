package com.example.application.mail_module.controller;

import com.example.application.common.email.EmailService;
import com.example.application.common.email.EmailTemplateService;
import com.example.application.common.email.SendResult;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.mail_module.dto.MailSettingsOverview;
import com.example.application.mail_module.dto.MailSettingsRequest;
import com.example.application.mail_module.dto.MailSettingsResponse;
import com.example.application.mail_module.dto.MailTestRequest;
import com.example.application.mail_module.dto.MailTestResponse;
import com.example.application.mail_module.service.EffectiveMailConfig;
import com.example.application.mail_module.service.MailSettingsService;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Outgoing-email senders, managed PER CLIENT COMPANY. Every endpoint takes an optional
 * {@code clientCompanyId}: given, it addresses that company's own sender; omitted, the platform
 * default that companies without their own fall back to.
 *
 * Super Admin only: MAIL_SETTINGS_MANAGE is granted to no other role, so neither a company's admins
 * nor any other user can see, add or change a sender - for their own company or anyone else's.
 * No endpoint here ever returns a password.
 */
@RestController
@RequestMapping("/api/admin/mail-settings")
public class MailSettingsController {

    private final MailSettingsService settingsService;
    private final EmailService emailService;
    private final EmailTemplateService templateService;

    public MailSettingsController(MailSettingsService settingsService, EmailService emailService,
                                  EmailTemplateService templateService) {
        this.settingsService = settingsService;
        this.emailService = emailService;
        this.templateService = templateService;
    }

    /** The table: the platform default plus every client company and which sender its mail goes out from. */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('MAIL_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<MailSettingsOverview>> overview() {
        return ResponseEntity.ok(ApiResponse.success("OK", settingsService.overview()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MAIL_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<MailSettingsResponse>> get(@RequestParam(required = false) Long clientCompanyId) {
        return ResponseEntity.ok(ApiResponse.success("OK", settingsService.view(clientCompanyId)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('MAIL_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<MailSettingsResponse>> save(@RequestParam(required = false) Long clientCompanyId,
                                                                  @RequestBody MailSettingsRequest request,
                                                                  @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Email sender saved",
                settingsService.save(clientCompanyId, request, principal.getId(), httpRequest)));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('MAIL_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<MailSettingsResponse>> delete(@RequestParam(required = false) Long clientCompanyId,
                                                                    @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Saved email sender deleted",
                settingsService.delete(clientCompanyId, principal.getId(), httpRequest)));
    }

    /** Sends a real message exactly the way that company's real emails go out, and reports WHY if it fails - so a sender can be verified from the UI instead of from server logs. */
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('MAIL_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<MailTestResponse>> test(@RequestParam(required = false) Long clientCompanyId,
                                                              @RequestBody MailTestRequest request) {
        String to = request.toAddress() == null ? "" : request.toAddress().trim();
        try {
            new InternetAddress(to, true).validate();
        } catch (AddressException e) {
            throw new BadRequestException("Enter a valid email address to send the test to.");
        }
        String scopeName = settingsService.scopeName(clientCompanyId);   // 404 for an unknown company
        EffectiveMailConfig cfg = settingsService.resolveEffective(clientCompanyId);
        String html = templateService.render("mail-test", Map.of(
                "source", switch (cfg.source()) {
                    case MailSettingsService.SOURCE_COMPANY -> scopeName + "'s own sender (Email Settings)";
                    case MailSettingsService.SOURCE_PLATFORM -> "the platform default sender (Email Settings)";
                    default -> "the server environment variables (MAIL_*)";
                },
                "sender", cfg.fromAddress() == null ? "" : cfg.fromAddress()));
        SendResult result = emailService.send(clientCompanyId, to, "WORKZEN - email settings test (" + scopeName + ")", html);
        MailTestResponse body = result.sent()
                ? new MailTestResponse(true, "Test email sent to " + to + " from " + cfg.fromAddress() + ". Check the inbox (and the Spam folder).")
                : new MailTestResponse(false, result.failureReason());
        return ResponseEntity.ok(ApiResponse.success(result.sent() ? "Test email sent" : "Test email failed", body));
    }
}
