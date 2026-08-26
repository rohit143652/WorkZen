package com.example.application.site_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.site_module.dto.SiteRequest;
import com.example.application.site_module.dto.SiteResponse;
import com.example.application.site_module.service.SiteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SITE_READ')")
    public ResponseEntity<ApiResponse<Page<SiteResponse>>> findAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", siteService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SITE_READ')")
    public ResponseEntity<ApiResponse<SiteResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", siteService.findById(id)));
    }

    /** Preview of the code the Add form should show (disabled) before the user even submits. */
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('SITE_CREATE')")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> nextCode() {
        return ResponseEntity.ok(ApiResponse.success("OK", java.util.Map.of("code", siteService.previewNextCode())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SITE_CREATE')")
    public ResponseEntity<ApiResponse<SiteResponse>> create(@Valid @RequestBody SiteRequest request,
                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                              HttpServletRequest httpRequest) {
        SiteResponse created = siteService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Site created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SITE_UPDATE')")
    public ResponseEntity<ApiResponse<SiteResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody SiteRequest request,
                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Site updated successfully",
                siteService.update(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SITE_ACTIVATE')")
    public ResponseEntity<ApiResponse<SiteResponse>> activate(@PathVariable Long id,
                                                                @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Site activated",
                siteService.setStatus(id, "ACTIVE", principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('SITE_DEACTIVATE')")
    public ResponseEntity<ApiResponse<SiteResponse>> deactivate(@PathVariable Long id,
                                                                  @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Site deactivated",
                siteService.setStatus(id, "INACTIVE", principal.getId(), httpRequest)));
    }
}
