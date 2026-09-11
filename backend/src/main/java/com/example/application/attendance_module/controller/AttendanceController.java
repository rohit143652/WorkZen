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

    /** "My Attendance History" - for anyone with ATTENDANCE_SELF_MARK, regardless of whether
        they also hold ATTENDANCE_READ (most self-service employees don't) - resolves strictly
        to the CALLER's own records, see AttendanceService.findMyHistoryInRange(). */
    @GetMapping("/mine/history")
    @PreAuthorize("hasAuthority('ATTENDANCE_SELF_MARK')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> myHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("OK", attendanceService.findMyHistoryInRange(principal.getId(), from, to)));
    }

    /**
     * On-demand selfie fetch - deliberately NOT bundled into any list/detail response (see
     * AttendanceResponse javadoc on hasCheckInSelfie/hasCheckOutSelfie). No @PreAuthorize here:
     * whether this call is allowed depends on WHOSE attendance record it is (own record: always
     * allowed; someone else's: needs ATTENDANCE_READ), which is exactly the kind of per-row
     * check @PreAuthorize's static role/permission expressions can't express - see
     * AttendanceService.getSelfie() for the actual authorization logic.
     */
    @GetMapping("/{id}/selfie")
    public ResponseEntity<ApiResponse<String>> getSelfie(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean checkOut,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("OK", attendanceService.getSelfie(id, checkOut, principal.getId())));
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

    /** Today's centralized attendance record for the logged-in employee - check-in/out times,
        working duration, late/early-exit, work mode, source, all in one place for the "Today's
        Attendance" card. Returns null (as data) if nothing recorded yet today. Reuses
        ATTENDANCE_SELF_MARK - see V99 migration note on why check-in/out don't need a separate permission. */
    @GetMapping("/today")
    @PreAuthorize("hasAuthority('ATTENDANCE_SELF_MARK')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> today(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("OK", attendanceService.findMyTodayStatus(principal.getId())));
    }

    /** Company-wide "who's checked in today" snapshot for the admin dashboard - see AttendanceService.getTodayOverview(). */
    @GetMapping("/today-overview")
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    public ResponseEntity<ApiResponse<TodayAttendanceOverviewResponse>> todayOverview() {
        return ResponseEntity.ok(ApiResponse.success("OK", attendanceService.getTodayOverview()));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('ATTENDANCE_SELF_MARK')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(@RequestBody(required = false) CheckInRequest request,
                                                                     @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                     HttpServletRequest httpRequest) {
        CheckInRequest body = request != null ? request : new CheckInRequest();
        AttendanceResponse result = attendanceService.checkIn(principal.getId(), body, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Checked in successfully", result));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAuthority('ATTENDANCE_SELF_MARK')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(@RequestBody(required = false) CheckOutRequest request,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                      HttpServletRequest httpRequest) {
        CheckOutRequest body = request != null ? request : new CheckOutRequest();
        AttendanceResponse result = attendanceService.checkOut(principal.getId(), body, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Checked out successfully", result));
    }
}
