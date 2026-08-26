package com.example.application.advance_module.controller;

import com.example.application.advance_module.dto.AdvanceDashboardSummaryResponse;
import com.example.application.advance_module.dto.EmployeeAdvanceResponse;
import com.example.application.advance_module.service.EmployeeAdvanceService;
import com.example.application.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tenant-wide Advance Dashboard (architecture completion audit, spec
 * section 12) - CLIENT_ADMIN only, same ADVANCE_READ permission as the
 * per-employee advance screens. Genuinely missing before this: advances
 * were only ever visible one employee at a time, embedded in Employee
 * Details - there was no single screen showing every advance across the
 * whole company with totals.
 */
@RestController
@RequestMapping("/api/advances")
public class AdvanceDashboardController {

    private final EmployeeAdvanceService advanceService;

    public AdvanceDashboardController(EmployeeAdvanceService advanceService) {
        this.advanceService = advanceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<ApiResponse<List<EmployeeAdvanceResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("OK", advanceService.listAllForTenant()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<ApiResponse<AdvanceDashboardSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success("OK", advanceService.getDashboardSummary()));
    }
}
