package com.example.application.leave_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.leave_module.dto.EmployeeLeaveSummaryResponse;
import com.example.application.leave_module.dto.PaidLeaveConfigRequest;
import com.example.application.leave_module.dto.PaidLeaveConfigResponse;
import com.example.application.leave_module.service.EmployeePaidLeaveService;
import com.example.application.leave_module.service.PaidLeaveConfigService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Leave policy - effective-dated (architecture refactor Phase 9, mirroring payroll_module.PayrollSettingsController). */
@RestController
@RequestMapping("/api/paid-leave/config")
public class PaidLeaveConfigController {

    private final PaidLeaveConfigService configService;
    private final EmployeePaidLeaveService paidLeaveService;

    public PaidLeaveConfigController(PaidLeaveConfigService configService, EmployeePaidLeaveService paidLeaveService) {
        this.configService = configService;
        this.paidLeaveService = paidLeaveService;
    }

    /** The policy in effect today. */
    @GetMapping
    @PreAuthorize("hasAuthority('PAID_LEAVE_CONFIG_READ')")
    public ResponseEntity<ApiResponse<PaidLeaveConfigResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("OK", configService.getForCurrentTenant()));
    }

    /** Full policy timeline, newest first. */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('PAID_LEAVE_CONFIG_READ')")
    public ResponseEntity<ApiResponse<List<PaidLeaveConfigResponse>>> history() {
        return ResponseEntity.ok(ApiResponse.success("OK", configService.getHistory()));
    }

    /** What would apply to a specific leave month, past or future. */
    @GetMapping("/for-month")
    @PreAuthorize("hasAuthority('PAID_LEAVE_CONFIG_READ')")
    public ResponseEntity<ApiResponse<PaidLeaveConfigResponse>> forMonth(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success("OK", configService.getForMonth(year, month)));
    }

    /** Schedules a new policy (today or a future effectiveFrom) - never edits a past/current policy in place. */
    @PostMapping
    @PreAuthorize("hasAuthority('PAID_LEAVE_CONFIG_UPDATE')")
    public ResponseEntity<ApiResponse<PaidLeaveConfigResponse>> create(
            @Valid @RequestBody PaidLeaveConfigRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(201).body(ApiResponse.success("Leave policy scheduled",
                configService.createFutureConfig(request, principal.getId(), httpRequest)));
    }

    /** Edits a policy that hasn't taken effect yet. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAID_LEAVE_CONFIG_UPDATE')")
    public ResponseEntity<ApiResponse<PaidLeaveConfigResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PaidLeaveConfigRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Leave policy updated",
                configService.updateFutureConfig(id, request, principal.getId(), httpRequest)));
    }

    /** Cancels a not-yet-effective policy - automatically reopens whichever policy it had closed. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAID_LEAVE_CONFIG_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        configService.cancelFutureConfig(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Leave policy cancelled"));
    }

    /** Every active employee's current total available paid leave - for the Paid Leave Settings overview. */
    @GetMapping("/employee-balances")
    @PreAuthorize("hasAuthority('PAID_LEAVE_READ')")
    public ResponseEntity<ApiResponse<List<EmployeeLeaveSummaryResponse>>> listEmployeeBalances() {
        return ResponseEntity.ok(ApiResponse.success("OK", paidLeaveService.listAllEmployeeBalances()));
    }
}
