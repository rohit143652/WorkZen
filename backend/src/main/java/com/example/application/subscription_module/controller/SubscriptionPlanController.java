package com.example.application.subscription_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.subscription_module.dto.SubscriptionPlanRequest;
import com.example.application.subscription_module.dto.SubscriptionPlanResponse;
import com.example.application.subscription_module.service.SubscriptionPlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Super Admin only - see SUBSCRIPTION_PLAN_* permissions (V110 migration). Never exposed to a tenant/client user. */
@RestController
@RequestMapping("/api/subscription-plans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    public SubscriptionPlanController(SubscriptionPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTION_PLAN_READ')")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> list(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success("OK", activeOnly ? planService.findAllActive() : planService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_PLAN_READ')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", planService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTION_PLAN_CREATE')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> create(
            @Valid @RequestBody SubscriptionPlanRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        return ResponseEntity.status(201).body(ApiResponse.success("Plan created", planService.create(request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_PLAN_UPDATE')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> update(
            @PathVariable Long id, @Valid @RequestBody SubscriptionPlanRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Plan updated", planService.update(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_PLAN_ACTIVATE')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> activate(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Plan activated", planService.setActive(id, true, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_PLAN_DEACTIVATE')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> deactivate(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Plan deactivated", planService.setActive(id, false, principal.getId(), httpRequest)));
    }
}
