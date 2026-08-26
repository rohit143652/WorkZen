package com.example.application.advance_module.controller;

import com.example.application.advance_module.dto.AdvanceGrantRequest;
import com.example.application.advance_module.dto.AdvancePartialSettlementRequest;
import com.example.application.advance_module.dto.AdvanceRecoveryAmountRequest;
import com.example.application.advance_module.dto.AdvanceRecoverViaPayrollRequest;
import com.example.application.advance_module.dto.AdvanceRecoveryTransactionResponse;
import com.example.application.advance_module.dto.EmployeeAdvanceResponse;
import com.example.application.advance_module.service.EmployeeAdvanceService;
import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CLIENT_ADMIN only - granting/adjusting advances directly affects payroll deductions. */
@RestController
@RequestMapping("/api/employees/{employeeId}/advances")
public class EmployeeAdvanceController {

    private final EmployeeAdvanceService advanceService;

    public EmployeeAdvanceController(EmployeeAdvanceService advanceService) {
        this.advanceService = advanceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<ApiResponse<List<EmployeeAdvanceResponse>>> list(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("OK", advanceService.listAdvances(employeeId)));
    }

    /** Every recovery event for one advance - "which payroll (or manual payment) recovered this amount?" */
    @GetMapping("/{advanceId}/recovery-history")
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<ApiResponse<List<AdvanceRecoveryTransactionResponse>>> recoveryHistory(
            @PathVariable Long employeeId, @PathVariable Long advanceId) {
        return ResponseEntity.ok(ApiResponse.success("OK", advanceService.getRecoveryHistory(employeeId, advanceId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADVANCE_GRANT')")
    public ResponseEntity<ApiResponse<EmployeeAdvanceResponse>> grant(
            @PathVariable Long employeeId,
            @Valid @RequestBody AdvanceGrantRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        EmployeeAdvanceResponse created = advanceService.grantAdvance(employeeId, request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Advance granted", created));
    }

    @PutMapping("/{advanceId}/recovery-amount")
    @PreAuthorize("hasAuthority('ADVANCE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeAdvanceResponse>> updateRecoveryAmount(
            @PathVariable Long employeeId, @PathVariable Long advanceId,
            @Valid @RequestBody AdvanceRecoveryAmountRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Recovery amount updated",
                advanceService.updateRecoveryAmount(employeeId, advanceId, request, principal.getId(), httpRequest)));
    }

    /** Pauses or resumes payroll-based recovery for this advance - "should this month's payroll cut this advance or not". Manual settlement is unaffected either way. */
    @PutMapping("/{advanceId}/recover-via-payroll")
    @PreAuthorize("hasAuthority('ADVANCE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeAdvanceResponse>> updateRecoverViaPayroll(
            @PathVariable Long employeeId, @PathVariable Long advanceId,
            @Valid @RequestBody AdvanceRecoverViaPayrollRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Payroll recovery setting updated",
                advanceService.updateRecoverViaPayroll(employeeId, advanceId, request.getRecoverViaPayroll(), principal.getId(), httpRequest)));
    }

    @PutMapping("/{advanceId}/settle")
    @PreAuthorize("hasAuthority('ADVANCE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeAdvanceResponse>> settle(
            @PathVariable Long employeeId, @PathVariable Long advanceId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Advance settled",
                advanceService.settleAdvance(employeeId, advanceId, principal.getId(), httpRequest)));
    }

    /** Partial manual settlement - the employee paid part of the outstanding amount directly, outside payroll. */
    @PutMapping("/{advanceId}/settle-partial")
    @PreAuthorize("hasAuthority('ADVANCE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeAdvanceResponse>> settlePartial(
            @PathVariable Long employeeId, @PathVariable Long advanceId,
            @Valid @RequestBody AdvancePartialSettlementRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Partial settlement recorded",
                advanceService.settlePartial(employeeId, advanceId, request.getAmount(), request.getRemark(), principal.getId(), httpRequest)));
    }
}
