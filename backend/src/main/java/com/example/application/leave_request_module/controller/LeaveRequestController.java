package com.example.application.leave_request_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.leave_request_module.dto.LeaveRequestAdminCreateRequest;
import com.example.application.leave_request_module.dto.LeaveRequestCreateRequest;
import com.example.application.leave_request_module.dto.LeaveRequestResponse;
import com.example.application.leave_request_module.dto.LeaveRequestReviewRequest;
import com.example.application.leave_request_module.service.LeaveRequestService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Leave Request Workflow - see LeaveRequestService javadoc for the full self-request vs admin-direct-add flow. */
@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_READ')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", leaveRequestService.findAll()));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_SELF_CREATE')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> findMine(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("OK", leaveRequestService.findMine(principal.getId())));
    }

    @PostMapping("/mine")
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_SELF_CREATE')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> selfCreate(@Valid @RequestBody LeaveRequestCreateRequest request,
                                                                          @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                          HttpServletRequest httpRequest) {
        LeaveRequestResponse created = leaveRequestService.selfCreate(request, principal.getId(), principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Leave request submitted - awaiting approval", created));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> adminCreate(@Valid @RequestBody LeaveRequestAdminCreateRequest request,
                                                                           @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                           HttpServletRequest httpRequest) {
        LeaveRequestResponse created = leaveRequestService.adminCreate(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Leave added and marked on attendance", created));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approve(@PathVariable Long id,
                                                                       @RequestBody(required = false) LeaveRequestReviewRequest request,
                                                                       @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                       HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Leave request approved",
                leaveRequestService.approve(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> reject(@PathVariable Long id,
                                                                      @RequestBody(required = false) LeaveRequestReviewRequest request,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                      HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Leave request rejected",
                leaveRequestService.reject(id, request, principal.getId(), httpRequest)));
    }
}
