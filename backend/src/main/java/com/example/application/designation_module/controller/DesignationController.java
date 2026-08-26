package com.example.application.designation_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.designation_module.dto.DesignationRequest;
import com.example.application.designation_module.dto.DesignationResponse;
import com.example.application.designation_module.service.DesignationService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/designations")
public class DesignationController {

    private final DesignationService designationService;

    public DesignationController(DesignationService designationService) {
        this.designationService = designationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DESIGNATION_READ')")
    public ResponseEntity<ApiResponse<List<DesignationResponse>>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success("OK", designationService.findAll(includeInactive)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DESIGNATION_MANAGE')")
    public ResponseEntity<ApiResponse<DesignationResponse>> create(@Valid @RequestBody DesignationRequest request,
                                                                    @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                    HttpServletRequest httpRequest) {
        DesignationResponse created = designationService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Designation added successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DESIGNATION_MANAGE')")
    public ResponseEntity<ApiResponse<DesignationResponse>> rename(@PathVariable Long id,
                                                                    @Valid @RequestBody DesignationRequest request,
                                                                    @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Designation updated successfully",
                designationService.rename(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('DESIGNATION_MANAGE')")
    public ResponseEntity<ApiResponse<DesignationResponse>> activate(@PathVariable Long id,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                      HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Designation activated",
                designationService.setStatus(id, "ACTIVE", principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DESIGNATION_MANAGE')")
    public ResponseEntity<ApiResponse<DesignationResponse>> deactivate(@PathVariable Long id,
                                                                        @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Designation deactivated",
                designationService.setStatus(id, "INACTIVE", principal.getId(), httpRequest)));
    }
}
