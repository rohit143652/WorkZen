package com.example.application.attendance_module.controller;

import com.example.application.attendance_module.dto.AttendanceRuleConfigResponse;
import com.example.application.attendance_module.dto.UpdateAttendanceRuleConfigRequest;
import com.example.application.attendance_module.service.AttendanceRuleConfigService;
import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Company-wide attendance policy (office hours, grace period, half/full-day thresholds, weekly
    off) - read by anyone who can see attendance at all, changed only by ATTENDANCE_RULES_MANAGE.
    Flat "/api/attendance/rules" path (no literal "/admin/" prefix), same convention as every
    other controller in this codebase - access is controlled by permission, not by URL shape. */
@RestController
@RequestMapping("/api/attendance/rules")
public class AttendanceRuleConfigController {

    private final AttendanceRuleConfigService ruleConfigService;

    public AttendanceRuleConfigController(AttendanceRuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_READ') or hasAuthority('ATTENDANCE_SELF_MARK')")
    public ResponseEntity<ApiResponse<AttendanceRuleConfigResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("OK", ruleConfigService.getForCurrentTenant()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_RULES_MANAGE')")
    public ResponseEntity<ApiResponse<AttendanceRuleConfigResponse>> update(
            @Valid @RequestBody UpdateAttendanceRuleConfigRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Attendance rules updated",
                ruleConfigService.update(request, principal.getId(), httpRequest)));
    }
}
