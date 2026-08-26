package com.example.application.payroll_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.payroll_module.dto.PayrollAdjustmentRequest;
import com.example.application.payroll_module.dto.PayrollRunCancelRequest;
import com.example.application.payroll_module.dto.PayrollRunCreateRequest;
import com.example.application.payroll_module.dto.PayrollRunEmployeeResponse;
import com.example.application.payroll_module.dto.PayrollRunReopenRequest;
import com.example.application.payroll_module.dto.PayrollRunResponse;
import com.example.application.payroll_module.service.PayrollRunService;
import com.example.application.payroll_module.service.SalaryRegisterExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Persisted Payroll Run workflow (architecture refactor Phase 2) -
 * CLIENT_ADMIN only, same tenant-scoped-only convention as every other
 * payroll-adjacent controller. Every write here (create/calculate/approve/
 * pay/cancel) is explicit and audited; GET endpoints are pure reads of
 * already-persisted data and never trigger a recalculation.
 */
@RestController
@RequestMapping("/api/payroll/runs")
public class PayrollRunController {

    private final PayrollRunService payrollRunService;
    private final SalaryRegisterExportService salaryRegisterExportService;

    public PayrollRunController(PayrollRunService payrollRunService, SalaryRegisterExportService salaryRegisterExportService) {
        this.payrollRunService = payrollRunService;
        this.salaryRegisterExportService = salaryRegisterExportService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PAYROLL_RUN_CREATE')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> create(
            @Valid @RequestBody PayrollRunCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        PayrollRunResponse created = payrollRunService.createRun(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Payroll run created", created));
    }

    @PostMapping("/{id}/calculate")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_CALCULATE')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> calculate(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Payroll run calculated", payrollRunService.calculateRun(id, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_APPROVE')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Payroll run approved", payrollRunService.approveRun(id, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/mark-paid")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_PAY')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> markPaid(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Payroll run marked as paid", payrollRunService.markPaid(id, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_CANCEL')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody PayrollRunCancelRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Payroll run cancelled",
                payrollRunService.cancelRun(id, request.getCancellationReason(), principal.getId(), httpRequest)));
    }

    /**
     * Controlled reopen of an APPROVED run back to CALCULATED - gated by its own permission,
     * separate from ordinary approve/calculate, since this reverses a decision that was
     * already made. PAID payroll is rejected with a distinct message, never silently reopened.
     */
    @PutMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_REOPEN')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> reopen(
            @PathVariable Long id,
            @Valid @RequestBody PayrollRunReopenRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Payroll run reopened",
                payrollRunService.reopenRun(id, request.getReopenReason(), principal.getId(), httpRequest)));
    }

    /** Sets the manual Advance/Uniform deduction and Allowance for one employee - takes effect on the next Calculate. */
    @PutMapping("/{id}/employees/{employeeId}/adjustment")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_CALCULATE')")
    public ResponseEntity<ApiResponse<Void>> setEmployeeAdjustment(
            @PathVariable Long id, @PathVariable Long employeeId,
            @Valid @RequestBody PayrollAdjustmentRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        payrollRunService.setEmployeeAdjustment(id, employeeId, request.getOtherManualDeduction(), request.getAllowance(),
                principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Adjustment saved - recalculate this run to apply it"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAYROLL_RUN_READ')")
    public ResponseEntity<ApiResponse<Page<PayrollRunResponse>>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", payrollRunService.listRuns(year, month, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_READ')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", payrollRunService.getRun(id)));
    }

    @GetMapping("/{id}/employees")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_READ')")
    public ResponseEntity<ApiResponse<Page<PayrollRunEmployeeResponse>>> getEmployees(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", payrollRunService.getRunEmployees(id, pageable)));
    }

    /** The classic "Salary Register" Excel layout - read-only, built entirely from this run's already-persisted PayrollRunEmployee figures. */
    @GetMapping("/{id}/export/salary-register")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_READ')")
    public ResponseEntity<byte[]> exportSalaryRegister(@PathVariable Long id) {
        byte[] workbook = salaryRegisterExportService.generate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Salary-Register-" + id + ".xlsx\"")
                .body(workbook);
    }
}
