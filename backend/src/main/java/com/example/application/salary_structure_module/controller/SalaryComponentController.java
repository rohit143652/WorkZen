package com.example.application.salary_structure_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.salary_structure_module.dto.SalaryComponentRequest;
import com.example.application.salary_structure_module.dto.SalaryComponentResponse;
import com.example.application.salary_structure_module.service.SalaryComponentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-components")
public class SalaryComponentController {

    private final SalaryComponentService componentService;

    public SalaryComponentController(SalaryComponentService componentService) {
        this.componentService = componentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_READ')")
    public ResponseEntity<ApiResponse<List<SalaryComponentResponse>>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success("OK", componentService.findAll(includeInactive)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_CREATE')")
    public ResponseEntity<ApiResponse<SalaryComponentResponse>> create(@Valid @RequestBody SalaryComponentRequest request,
                                                                         @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                         HttpServletRequest httpRequest) {
        SalaryComponentResponse created = componentService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Salary component added successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_UPDATE')")
    public ResponseEntity<ApiResponse<SalaryComponentResponse>> update(@PathVariable Long id,
                                                                         @Valid @RequestBody SalaryComponentRequest request,
                                                                         @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                         HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Salary component updated successfully",
                componentService.update(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_UPDATE')")
    public ResponseEntity<ApiResponse<SalaryComponentResponse>> activate(@PathVariable Long id,
                                                                           @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                           HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Salary component activated",
                componentService.setStatus(id, true, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_UPDATE')")
    public ResponseEntity<ApiResponse<SalaryComponentResponse>> deactivate(@PathVariable Long id,
                                                                             @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                             HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Salary component deactivated",
                componentService.setStatus(id, false, principal.getId(), httpRequest)));
    }
}
