package com.example.application.payroll_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.payroll_module.dto.OvertimeRecordRequest;
import com.example.application.payroll_module.dto.OvertimeRecordResponse;
import com.example.application.payroll_module.service.EmployeeOvertimeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * The Overtime Register - see EmployeeOvertimeRecord/EmployeeOvertimeService for why this
 * exists (day-wise, auditable log; a month's payroll total is derived from these rows, not
 * typed in directly). OVERTIME_RECORD_MANAGE can log/edit/delete; OVERTIME_RECORD_READ can only
 * view - same split pattern as ATTENDANCE_CORRECTION_REQUEST/REVIEW. Both are meaningless if the
 * company's own OVERTIME_MANAGEMENT feature is off (checked inside the service, not just here).
 */
@RestController
@RequestMapping("/api/overtime-records")
public class EmployeeOvertimeController {

    private final EmployeeOvertimeService overtimeService;

    public EmployeeOvertimeController(EmployeeOvertimeService overtimeService) {
        this.overtimeService = overtimeService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OVERTIME_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<OvertimeRecordResponse>> mark(@Valid @RequestBody OvertimeRecordRequest request,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Overtime recorded successfully", overtimeService.markOvertime(request, principal.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OVERTIME_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @RequestParam Long employeeId) {
        overtimeService.deleteOvertime(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Overtime record deleted successfully", null));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('OVERTIME_RECORD_READ') or hasAuthority('OVERTIME_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<List<OvertimeRecordResponse>>> forEmployee(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("OK", overtimeService.getForEmployeeInRange(employeeId, from, to)));
    }
}
