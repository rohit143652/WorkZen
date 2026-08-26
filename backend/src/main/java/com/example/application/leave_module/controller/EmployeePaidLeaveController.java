package com.example.application.leave_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.leave_module.dto.EmployeePaidLeaveBalanceResponse;
import com.example.application.leave_module.dto.ExtraPaidLeaveRequest;
import com.example.application.leave_module.dto.ExtraPaidLeaveResponse;
import com.example.application.leave_module.service.EmployeePaidLeaveService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Read endpoints (balance/history/extra-history) allow either PAID_LEAVE_READ
 * or the caller viewing their own linked employee record - see
 * EmployeePaidLeaveService.assertReadAccess(). Write endpoints (grant/update/
 * cancel) always require the corresponding admin permission; an employee can
 * never grant or modify their own (or anyone's) leave (spec section 10).
 */
@RestController
@RequestMapping("/api/employees/{employeeId}/paid-leave")
public class EmployeePaidLeaveController {

    private final EmployeePaidLeaveService paidLeaveService;

    public EmployeePaidLeaveController(EmployeePaidLeaveService paidLeaveService) {
        this.paidLeaveService = paidLeaveService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<EmployeePaidLeaveBalanceResponse>> getCurrentBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                paidLeaveService.getEmployeeLeaveBalance(employeeId, LocalDate.now())));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<EmployeePaidLeaveBalanceResponse>>> getHistory(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("OK", paidLeaveService.getEmployeeLeaveHistory(employeeId)));
    }

    @GetMapping("/extra")
    public ResponseEntity<ApiResponse<List<ExtraPaidLeaveResponse>>> listExtraLeave(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("OK", paidLeaveService.listExtraLeaveHistory(employeeId)));
    }

    @PostMapping("/extra")
    @PreAuthorize("hasAuthority('PAID_LEAVE_GRANT')")
    public ResponseEntity<ApiResponse<ExtraPaidLeaveResponse>> grantExtraLeave(
            @PathVariable Long employeeId,
            @Valid @RequestBody ExtraPaidLeaveRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        ExtraPaidLeaveResponse created = paidLeaveService.grantExtraLeave(employeeId, request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Extra paid leave granted", created));
    }

    @PutMapping("/extra/{id}")
    @PreAuthorize("hasAuthority('PAID_LEAVE_UPDATE')")
    public ResponseEntity<ApiResponse<ExtraPaidLeaveResponse>> updateExtraLeave(
            @PathVariable Long employeeId,
            @PathVariable Long id,
            @Valid @RequestBody ExtraPaidLeaveRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Extra paid leave updated",
                paidLeaveService.updateExtraLeave(employeeId, id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/extra/{id}/cancel")
    @PreAuthorize("hasAuthority('PAID_LEAVE_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> cancelExtraLeave(
            @PathVariable Long employeeId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        paidLeaveService.cancelExtraLeave(employeeId, id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Extra paid leave cancelled"));
    }
}
