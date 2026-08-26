package com.example.application.salary_structure_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.salary_structure_module.dto.SalaryStructureRequest;
import com.example.application.salary_structure_module.dto.SalaryStructureResponse;
import com.example.application.salary_structure_module.service.SalaryStructureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salary-structures")
public class SalaryStructureController {

    private final SalaryStructureService structureService;

    public SalaryStructureController(SalaryStructureService structureService) {
        this.structureService = structureService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_READ')")
    public ResponseEntity<ApiResponse<Page<SalaryStructureResponse>>> findAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", structureService.findAll(pageable)));
    }

    /** Unpaged ACTIVE-only list for pickers, e.g. the Employee form's Salary Structure dropdown. Gated by SALARY_STRUCTURE_READ, same as everything else here - EMPLOYEE_SALARY_UPDATE alone does not grant visibility into the structures catalog. */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_READ')")
    public ResponseEntity<ApiResponse<java.util.List<SalaryStructureResponse>>> findAllActive() {
        return ResponseEntity.ok(ApiResponse.success("OK", structureService.findAllActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_READ')")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", structureService.findById(id)));
    }

    /** Preview of the code the Add form should show (disabled) before the user even submits. */
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_CREATE')")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> nextCode() {
        return ResponseEntity.ok(ApiResponse.success("OK", java.util.Map.of("code", structureService.previewNextCode())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_CREATE')")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> create(@Valid @RequestBody SalaryStructureRequest request,
                                                                         @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                         HttpServletRequest httpRequest) {
        SalaryStructureResponse created = structureService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Salary structure created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_UPDATE')")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> update(@PathVariable Long id,
                                                                         @Valid @RequestBody SalaryStructureRequest request,
                                                                         @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                         HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Salary structure updated successfully",
                structureService.update(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_UPDATE')")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> activate(@PathVariable Long id,
                                                                           @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                           HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Salary structure activated",
                structureService.setStatus(id, "ACTIVE", principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_UPDATE')")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> deactivate(@PathVariable Long id,
                                                                             @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                             HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Salary structure deactivated",
                structureService.setStatus(id, "INACTIVE", principal.getId(), httpRequest)));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_CREATE')")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> duplicate(@PathVariable Long id,
                                                                            @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                            HttpServletRequest httpRequest) {
        SalaryStructureResponse copy = structureService.duplicate(id, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Salary structure duplicated successfully", copy));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        structureService.delete(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Salary structure deleted successfully"));
    }
}
