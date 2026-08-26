package com.example.application.payroll_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.payroll_module.dto.PayrollSettingsRequest;
import com.example.application.payroll_module.dto.PayrollSettingsResponse;
import com.example.application.payroll_module.service.PayrollSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Payroll configuration - effective-dated (architecture refactor Phase 8).
 * CLIENT_ADMIN only, same as the rest of the payroll-adjacent screens -
 * see PAYROLL_REGISTER_EXPORT. GET/POST/PUT/DELETE here never touch
 * PayrollRun/PayrollRunEmployee directly; PayrollRunService is the only
 * consumer of the resulting configuration (via PayrollSettingsResolver).
 */
@RestController
@RequestMapping("/api/payroll/settings")
public class PayrollSettingsController {

    private final PayrollSettingsService settingsService;

    public PayrollSettingsController(PayrollSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** The configuration in effect today. */
    @GetMapping
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<PayrollSettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("OK", settingsService.getForCurrentTenant()));
    }

    /** Full configuration timeline, newest first. */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<List<PayrollSettingsResponse>>> history() {
        return ResponseEntity.ok(ApiResponse.success("OK", settingsService.getHistory()));
    }

    /** What would apply to a specific payroll month, past or future - explains "why did August use 12%?". */
    @GetMapping("/for-month")
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<PayrollSettingsResponse>> forMonth(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success("OK", settingsService.getForMonth(year, month)));
    }

    /** Schedules a new configuration (today or a future effectiveFrom) - never edits a past/current rate in place. */
    @PostMapping
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<PayrollSettingsResponse>> create(
            @Valid @RequestBody PayrollSettingsRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(201).body(ApiResponse.success("Payroll configuration scheduled",
                settingsService.createFutureConfig(request, principal.getId(), httpRequest)));
    }

    /** Edits a configuration that hasn't taken effect yet - rejected once its effective date has passed. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<PayrollSettingsResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PayrollSettingsRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Payroll configuration updated",
                settingsService.updateFutureConfig(id, request, principal.getId(), httpRequest)));
    }

    /** Cancels a not-yet-effective configuration - automatically reopens whichever configuration it had closed, so future months are never left without an applicable configuration. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        settingsService.cancelFutureConfig(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Payroll configuration cancelled"));
    }
}
