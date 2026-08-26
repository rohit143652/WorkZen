package com.example.application.dashboard_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.dashboard_module.dto.DashboardSummaryResponse;
import com.example.application.dashboard_module.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('DASHBOARD_VIEW', 'DASHBOARD_ANALYTICS')")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success("OK", dashboardService.getSummary()));
    }
}
