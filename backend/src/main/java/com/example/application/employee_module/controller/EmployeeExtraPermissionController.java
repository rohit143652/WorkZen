package com.example.application.employee_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.employee_module.dto.ExtraPermissionsRequest;
import com.example.application.employee_module.dto.ExtraPermissionsResponse;
import com.example.application.employee_module.service.EmployeeExtraPermissionService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Additional permissions for one employee, on top of their role. Admin-tier: USER_PERMISSION_MANAGE
 * is held by Super Admin / Client Admin / Admin only, and the service additionally limits what you
 * can hand out to what you hold yourself.
 */
@RestController
@RequestMapping("/api/employees/{employeeId}/extra-permissions")
public class EmployeeExtraPermissionController {

    private final EmployeeExtraPermissionService service;

    public EmployeeExtraPermissionController(EmployeeExtraPermissionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_PERMISSION_MANAGE')")
    public ResponseEntity<ApiResponse<ExtraPermissionsResponse>> get(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.view(employeeId)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('USER_PERMISSION_MANAGE')")
    public ResponseEntity<ApiResponse<ExtraPermissionsResponse>> update(@PathVariable Long employeeId,
                                                                        @RequestBody ExtraPermissionsRequest request,
                                                                        @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Additional permissions saved",
                service.update(employeeId, request, principal.getId(), httpRequest)));
    }
}
