package com.example.application.payroll_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.payroll_module.dto.ProfessionalTaxSlabRequest;
import com.example.application.payroll_module.dto.ProfessionalTaxSlabResponse;
import com.example.application.payroll_module.service.ProfessionalTaxSlabService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Professional Tax slab management - same CLIENT_ADMIN-only permission as Payroll Settings, since this is part of the same overall payroll configuration. */
@RestController
@RequestMapping("/api/payroll/pt-slabs")
public class ProfessionalTaxSlabController {

    private final ProfessionalTaxSlabService slabService;

    public ProfessionalTaxSlabController(ProfessionalTaxSlabService slabService) {
        this.slabService = slabService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<List<ProfessionalTaxSlabResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", slabService.findAll()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<ProfessionalTaxSlabResponse>> create(
            @RequestBody ProfessionalTaxSlabRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        return ResponseEntity.status(201).body(ApiResponse.success("Professional Tax slab created",
                slabService.create(request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<ProfessionalTaxSlabResponse>> update(
            @PathVariable Long id, @RequestBody ProfessionalTaxSlabRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Professional Tax slab updated",
                slabService.update(id, request, principal.getId(), httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_REGISTER_EXPORT')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        slabService.delete(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Professional Tax slab deleted"));
    }
}
