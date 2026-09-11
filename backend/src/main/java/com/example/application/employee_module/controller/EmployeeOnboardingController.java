package com.example.application.employee_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.employee_module.dto.OnboardingSetPasswordRequest;
import com.example.application.employee_module.dto.OnboardingValidateResponse;
import com.example.application.employee_module.dto.OnboardingVerifyCodeRequest;
import com.example.application.employee_module.service.EmployeeOnboardingService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Two very different audiences share this controller:
 *   - /validate, /verify-code, /set-password are PUBLIC (see SecurityConfig.PUBLIC_ENDPOINTS) -
 *     an invited employee has no JWT yet, that's the entire point of onboarding.
 *   - /resend-invitation/{employeeId} is an ADMIN action, behind the normal JWT + permission
 *     check like everything else in this app.
 * Multi-tenant safety for the public endpoints: nothing here ever accepts or trusts a
 * clientCompanyId from the request - the invitation row itself is the only source of which
 * tenant/employee/user this token belongs to (see EmployeeOnboardingService).
 */
@RestController
@RequestMapping("/api/onboarding")
public class EmployeeOnboardingController {

    private final EmployeeOnboardingService onboardingService;

    public EmployeeOnboardingController(EmployeeOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/validate/{token}")
    public ResponseEntity<ApiResponse<OnboardingValidateResponse>> validate(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success("OK", onboardingService.validateToken(token)));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@Valid @RequestBody OnboardingVerifyCodeRequest request) {
        onboardingService.verifyCode(request.getToken(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("Code verified", null));
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<Void>> setPassword(@Valid @RequestBody OnboardingSetPasswordRequest request) {
        onboardingService.setPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password set successfully - you can now log in", null));
    }

    @PostMapping("/resend-invitation/{employeeId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_ONBOARDING_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> resendInvitation(@PathVariable Long employeeId,
                                                                @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                HttpServletRequest httpRequest) {
        onboardingService.resendInvitation(employeeId, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Invitation resent successfully", null));
    }
}
