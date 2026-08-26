package com.example.application.role_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.role_module.dto.RolePermissionsRequest;
import com.example.application.role_module.dto.RoleRequest;
import com.example.application.role_module.dto.RoleResponse;
import com.example.application.role_module.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", roleService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<RoleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", roleService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request,
                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                              HttpServletRequest httpRequest) {
        RoleResponse created = roleService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Role created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody RoleRequest request,
                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Role updated",
                roleService.update(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> updatePermissions(@PathVariable Long id,
                                                                         @Valid @RequestBody RolePermissionsRequest request,
                                                                         @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                         HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Role permissions updated",
                roleService.updatePermissions(id, request, principal.getId(), httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        roleService.delete(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Role deleted"));
    }
}
