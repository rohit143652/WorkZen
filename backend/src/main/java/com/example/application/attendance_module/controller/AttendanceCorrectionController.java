package com.example.application.attendance_module.controller;

import com.example.application.attendance_module.dto.CorrectionRequestCreateRequest;
import com.example.application.attendance_module.dto.CorrectionRequestResponse;
import com.example.application.attendance_module.dto.CorrectionReviewRequest;
import com.example.application.attendance_module.service.AttendanceCorrectionService;
import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * One flat resource path for the whole correction-request lifecycle, same convention as
 * LeaveRequestController (which also has no separate "/admin/..." base path) rather than the
 * literal "/admin/attendance/correction/..." shape sketched in the original spec - employee vs.
 * admin actions are differentiated by permission and sub-path (/mine vs. the plain list, and
 * /{id}/approve|reject), not by URL prefix, consistent with how this codebase already does it
 * elsewhere.
 */
@RestController
@RequestMapping("/api/attendance/correction-requests")
public class AttendanceCorrectionController {

    private final AttendanceCorrectionService correctionService;

    public AttendanceCorrectionController(AttendanceCorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    /** Employee requesting a fix to their OWN attendance - see AttendanceCorrectionService.create() for the self-only guarantee. */
    @PostMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_CORRECTION_REQUEST')")
    public ResponseEntity<ApiResponse<CorrectionRequestResponse>> create(
            @Valid @RequestBody CorrectionRequestCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        CorrectionRequestResponse created = correctionService.create(principal.getId(), request, httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Correction request submitted", created));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('ATTENDANCE_CORRECTION_REQUEST')")
    public ResponseEntity<ApiResponse<Page<CorrectionRequestResponse>>> mine(
            @AuthenticationPrincipal CustomUserPrincipal principal, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", correctionService.findMine(principal.getId(), pageable)));
    }

    /** Admin/HR review queue - pass ?status=PENDING for just the ones awaiting action. */
    @GetMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_CORRECTION_REVIEW')")
    public ResponseEntity<ApiResponse<Page<CorrectionRequestResponse>>> all(
            @RequestParam(required = false) String status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", correctionService.findAll(status, pageable)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ATTENDANCE_CORRECTION_REVIEW')")
    public ResponseEntity<ApiResponse<CorrectionRequestResponse>> approve(
            @PathVariable Long id, @RequestBody(required = false) CorrectionReviewRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        CorrectionReviewRequest body = request != null ? request : new CorrectionReviewRequest();
        return ResponseEntity.ok(ApiResponse.success("Correction request approved",
                correctionService.approve(id, body, principal.getId(), httpRequest)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ATTENDANCE_CORRECTION_REVIEW')")
    public ResponseEntity<ApiResponse<CorrectionRequestResponse>> reject(
            @PathVariable Long id, @RequestBody(required = false) CorrectionReviewRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        CorrectionReviewRequest body = request != null ? request : new CorrectionReviewRequest();
        return ResponseEntity.ok(ApiResponse.success("Correction request rejected",
                correctionService.reject(id, body, principal.getId(), httpRequest)));
    }
}
