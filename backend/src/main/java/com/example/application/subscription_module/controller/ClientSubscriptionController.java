package com.example.application.subscription_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.subscription_module.dto.ClientSubscriptionRequest;
import com.example.application.subscription_module.dto.ClientSubscriptionResponse;
import com.example.application.subscription_module.dto.SubscriptionHistoryResponse;
import com.example.application.subscription_module.service.ClientSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Super Admin only - see CLIENT_SUBSCRIPTION_* permissions (V110 migration). A tenant/client user never sees or manages their own subscription in this phase (spec section 39). */
@RestController
@RequestMapping("/api/client-companies/{id}/subscription")
public class ClientSubscriptionController {

    private final ClientSubscriptionService subscriptionService;

    public ClientSubscriptionController(ClientSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_SUBSCRIPTION_READ')")
    public ResponseEntity<ApiResponse<ClientSubscriptionResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", subscriptionService.getForCompany(id)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CLIENT_SUBSCRIPTION_UPDATE')")
    public ResponseEntity<ApiResponse<ClientSubscriptionResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ClientSubscriptionRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Subscription updated", subscriptionService.update(id, request, principal.getId(), httpRequest)));
    }

    /** Quick "deactivate now" action - sets status to CANCELLED. Distinct from the daily auto-expiry job, which sets EXPIRED on its own once the end date passes. */
    @PutMapping("/deactivate")
    @PreAuthorize("hasAuthority('CLIENT_SUBSCRIPTION_MANAGE')")
    public ResponseEntity<ApiResponse<ClientSubscriptionResponse>> deactivate(
            @PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body,
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Subscription deactivated", subscriptionService.deactivate(id, reason, principal.getId(), httpRequest)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('CLIENT_SUBSCRIPTION_READ')")
    public ResponseEntity<ApiResponse<List<SubscriptionHistoryResponse>>> history(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", subscriptionService.getHistory(id)));
    }
}