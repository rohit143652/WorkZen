package com.example.application.client_company_module.controller;

import com.example.application.client_company_module.feature.FeatureAccessService;
import com.example.application.common.response.ApiResponse;
import com.example.application.common.tenant.TenantContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Not the Super Admin management surface (see ClientCompanyController's /features endpoints for
 * that) - this is what the frontend calls for ANY logged-in user to know which features their
 * own company currently has enabled, so it can hide a Check-In button etc. before ever hitting a
 * blocked API. No special permission beyond being authenticated at all - this only ever reveals
 * on/off flags for the caller's OWN tenant, never anyone else's, and never anything more
 * sensitive than "can I see this button".
 */
@RestController
@RequestMapping("/api/features")
public class FeatureController {

    private final FeatureAccessService featureAccessService;
    private final TenantContextService tenantContext;

    public FeatureController(FeatureAccessService featureAccessService, TenantContextService tenantContext) {
        this.featureAccessService = featureAccessService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> mine() {
        Long tenantId = tenantContext.currentTenantIdOrNull();
        // SUPER_ADMIN/global-scope accounts have no tenant, and therefore no feature
        // restrictions - returning an empty map is fine, the frontend treats "not in the map"
        // as enabled, same default as the backend (see FeatureAccessService).
        Map<String, Boolean> features = tenantId != null ? featureAccessService.getFeatureMap(tenantId) : Map.of();
        return ResponseEntity.ok(ApiResponse.success("OK", features));
    }

    /** The catalog structure itself (category -> codes), independent of any one company - used by the Subscription Plan form's feature checkboxes (a plan isn't tied to a company, so the per-company /mine endpoint above doesn't fit here). Same @PreAuthorize-free access as /mine: this only reveals which feature codes exist, nothing about any tenant's actual configuration. */
    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<com.example.application.client_company_module.feature.FeatureCode.FeatureCategory>>> catalog() {
        return ResponseEntity.ok(ApiResponse.success("OK", com.example.application.client_company_module.feature.FeatureCode.CATALOG));
    }
}
