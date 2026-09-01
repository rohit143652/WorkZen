package com.example.application.attendance_module.controller;

import com.example.application.attendance_module.dto.*;
import com.example.application.attendance_module.service.AttendanceService;
import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> mark(@Valid @RequestBody MarkAttendanceRequest request,
                                                                  @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                  HttpServletRequest httpRequest) {
        AttendanceResponse created = attendanceService.mark(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Attendance marked successfully", created));
    }

    /** For "Mark My Attendance" (self-service) - is today already marked? Returns null (as data) if not. */
    @GetMapping("/mine/today")
    @PreAuthorize("hasAuthority('ATTENDANCE_SELF_MARK')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> myTodayStatus(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("OK", attendanceService.findMyTodayStatus(principal.getId())));
    }

    /** One-click self-service attendance marking - see MarkMyAttendanceRequest/AttendanceService.markMine() for why this deliberately has no employeeId, date, or status fields. */
    @PostMapping("/mine")
    @PreAuthorize("hasAuthority('ATTENDANCE_SELF_MARK')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> markMine(@RequestBody(required = false) MarkMyAttendanceRequest request,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                      HttpServletRequest httpRequest) {
        var lat = request != null ? request.getLatitude() : null;
        var lng = request != null ? request.getLongitude() : null;
        AttendanceResponse created = attendanceService.markMine(principal.getId(), lat, lng, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Your attendance has been marked for today", created));
    }

    /**
     * Marks many employees at once for the same date - the answer to "100
     * employees would mean 100 saves". Each entry succeeds or fails on its
     * own; the response reports exactly how many were marked and why any
     * were skipped, so a bad row never blocks the rest of the batch.
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    public ResponseEntity<ApiResponse<BulkMarkAttendanceResult>> bulkMark(
            @Valid @RequestBody BulkMarkAttendanceRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        BulkMarkAttendanceResult result = attendanceService.bulkMark(request, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                result.getMarked() + " of " + result.getRequested() + " attendance records marked successfully", result));
    }

    /**
     * Editing an already-marked day. ATTENDANCE_UPDATE is granted only to
     * CLIENT_ADMIN (and the tenant-scoped ADMIN role that mirrors it) by
     * default - see V28 migration - so SITE_ADMIN/SITE_SUPERVISOR get a
     * clean 403 here, never a way to overwrite their own earlier entry.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_UPDATE')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> edit(@PathVariable Long id,
                                                                  @Valid @RequestBody UpdateAttendanceRequest request,
                                                                  @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Attendance updated successfully",
                attendanceService.edit(id, request, principal.getId(), httpRequest)));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> byEmployee(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("OK", attendanceService.findForEmployeeInRange(employeeId, from, to)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> findAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long siteId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", attendanceService.findAll(from, to, siteId, pageable)));
    }

    @GetMapping("/markable")
    @PreAuthorize("hasAuthority('ATTENDANCE_CREATE')")
    public ResponseEntity<ApiResponse<List<EmployeeAttendanceOption>>> markable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long siteId) {
        return ResponseEntity.ok(ApiResponse.success("OK", attendanceService.getMarkableEmployees(date, siteId)));
    }
}
