package com.example.application.exit_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.exit_module.dto.EmployeeExitRequest;
import com.example.application.exit_module.dto.EmployeeExitResponse;
import com.example.application.exit_module.service.ExitService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Exit Management (Full & Final Settlement) - CLIENT_ADMIN only by default (see V86 migration for the permission grants). */
@RestController
@RequestMapping("/api/employee-exits")
public class ExitController {

    private final ExitService exitService;

    public ExitController(ExitService exitService) {
        this.exitService = exitService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_EXIT_READ')")
    public ResponseEntity<ApiResponse<List<EmployeeExitResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", exitService.findAll()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_EXIT_CREATE')")
    public ResponseEntity<ApiResponse<EmployeeExitResponse>> initiate(@Valid @RequestBody EmployeeExitRequest request,
                                                                        @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                        HttpServletRequest httpRequest) {
        EmployeeExitResponse created = exitService.initiate(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Resignation recorded", created));
    }

    @GetMapping("/{id}/settlement-preview")
    @PreAuthorize("hasAuthority('EMPLOYEE_EXIT_SETTLE')")
    public ResponseEntity<ApiResponse<EmployeeExitResponse>> preview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", exitService.previewSettlement(id)));
    }

    @PostMapping("/{id}/settle")
    @PreAuthorize("hasAuthority('EMPLOYEE_EXIT_SETTLE')")
    public ResponseEntity<ApiResponse<EmployeeExitResponse>> settle(@PathVariable Long id,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                      HttpServletRequest httpRequest) {
        EmployeeExitResponse settled = exitService.settle(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Full & Final Settlement processed - employee deactivated", settled));
    }
}
