package com.example.application.permission_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.permission_module.dto.PermissionRequest;
import com.example.application.permission_module.dto.PermissionResponse;
import com.example.application.permission_module.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", permissionService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    public ResponseEntity<ApiResponse<PermissionResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", permissionService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    public ResponseEntity<ApiResponse<PermissionResponse>> create(@Valid @RequestBody PermissionRequest request,
                                                                    @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                    HttpServletRequest httpRequest) {
        PermissionResponse created = permissionService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Permission created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<ApiResponse<PermissionResponse>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody PermissionRequest request,
                                                                    @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Permission updated",
                permissionService.update(id, request, principal.getId(), httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        permissionService.delete(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Permission deleted"));
    }
}
