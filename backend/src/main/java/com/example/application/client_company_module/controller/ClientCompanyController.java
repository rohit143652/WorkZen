package com.example.application.client_company_module.controller;

import com.example.application.client_company_module.dto.ClientCompanyRequest;
import com.example.application.client_company_module.dto.ClientCompanyResponse;
import com.example.application.client_company_module.service.ClientCompanyService;
import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * SUPER_ADMIN-only tenant management. Every write here is the one place in
 * the system that legitimately creates/changes a Client Company - everything
 * downstream (employees, sites, sub-clients, assignments) only ever
 * *references* a clientCompanyId that was already established here.
 */
@RestController
@RequestMapping("/api/client-companies")
public class ClientCompanyController {

    private final ClientCompanyService clientCompanyService;

    public ClientCompanyController(ClientCompanyService clientCompanyService) {
        this.clientCompanyService = clientCompanyService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_COMPANY_READ')")
    public ResponseEntity<ApiResponse<Page<ClientCompanyResponse>>> findAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", clientCompanyService.findAll(pageable)));
    }

    /** Preview of the code the Add form should show (disabled) before the user even submits. */
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('CLIENT_COMPANY_CREATE')")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> nextCode() {
        return ResponseEntity.ok(ApiResponse.success("OK", java.util.Map.of("code", clientCompanyService.previewNextCode())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_COMPANY_READ')")
    public ResponseEntity<ApiResponse<ClientCompanyResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", clientCompanyService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_COMPANY_CREATE')")
    public ResponseEntity<ApiResponse<ClientCompanyResponse>> create(@Valid @RequestBody ClientCompanyRequest request,
                                                                       @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                       HttpServletRequest httpRequest) {
        ClientCompanyResponse created = clientCompanyService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Client company created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_COMPANY_UPDATE')")
    public ResponseEntity<ApiResponse<ClientCompanyResponse>> update(@PathVariable Long id,
                                                                       @Valid @RequestBody ClientCompanyRequest request,
                                                                       @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                       HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Client company updated successfully",
                clientCompanyService.update(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('CLIENT_COMPANY_ACTIVATE')")
    public ResponseEntity<ApiResponse<ClientCompanyResponse>> activate(@PathVariable Long id,
                                                                         @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                         HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Client company activated successfully",
                clientCompanyService.setStatus(id, "ACTIVE", principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('CLIENT_COMPANY_DEACTIVATE')")
    public ResponseEntity<ApiResponse<ClientCompanyResponse>> deactivate(@PathVariable Long id,
                                                                           @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                           HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Client company deactivated successfully",
                clientCompanyService.setStatus(id, "INACTIVE", principal.getId(), httpRequest)));
    }
}
