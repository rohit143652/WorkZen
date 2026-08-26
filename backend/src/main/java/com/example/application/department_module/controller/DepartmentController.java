package com.example.application.department_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.department_module.dto.DepartmentRequest;
import com.example.application.department_module.dto.DepartmentResponse;
import com.example.application.department_module.service.DepartmentService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_READ')")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success("OK", departmentService.findAll(includeInactive)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentRequest request,
                                                                    @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                    HttpServletRequest httpRequest) {
        DepartmentResponse created = departmentService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Department added successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> rename(@PathVariable Long id,
                                                                    @Valid @RequestBody DepartmentRequest request,
                                                                    @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Department updated successfully",
                departmentService.rename(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> activate(@PathVariable Long id,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                      HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Department activated",
                departmentService.setStatus(id, "ACTIVE", principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> deactivate(@PathVariable Long id,
                                                                        @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Department deactivated",
                departmentService.setStatus(id, "INACTIVE", principal.getId(), httpRequest)));
    }
}
