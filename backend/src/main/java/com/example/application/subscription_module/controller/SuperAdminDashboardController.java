package com.example.application.subscription_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.subscription_module.dto.SuperAdminDashboardResponse;
import com.example.application.subscription_module.service.ClientSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Super Admin's OWN dashboard - a dedicated, narrow endpoint rather than folding this into the
 * existing per-tenant dashboard data. The platform owner's dashboard is deliberately not the
 * same thing as a Client Admin's dashboard (attendance widgets, payroll, etc. mean nothing to
 * someone who manages the SaaS platform, not any one tenant's day-to-day operations) - see
 * ClientSubscriptionService.getDashboardSummary() for exactly what this returns and why.
 */
@RestController
@RequestMapping("/api/super-admin-dashboard")
public class SuperAdminDashboardController {

    private final ClientSubscriptionService subscriptionService;

    public SuperAdminDashboardController(ClientSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_COMPANY_READ')")
    public ResponseEntity<ApiResponse<SuperAdminDashboardResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success("OK", subscriptionService.getDashboardSummary()));
    }
}
