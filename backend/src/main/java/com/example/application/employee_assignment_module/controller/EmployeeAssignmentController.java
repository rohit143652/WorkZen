package com.example.application.employee_assignment_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.employee_assignment_module.dto.*;
import com.example.application.employee_assignment_module.service.EmployeeAssignmentService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EmployeeAssignmentController {

    private final EmployeeAssignmentService assignmentService;

    public EmployeeAssignmentController(EmployeeAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/api/employee-assignments")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGNMENT_READ')")
    public ResponseEntity<ApiResponse<Page<EmployeeAssignmentResponse>>> findAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", assignmentService.findAll(pageable)));
    }

    @GetMapping("/api/employee-assignments/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGNMENT_READ')")
    public ResponseEntity<ApiResponse<EmployeeAssignmentResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", assignmentService.findById(id)));
    }

    @PostMapping("/api/employee-assignments")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGN')")
    public ResponseEntity<ApiResponse<EmployeeAssignmentResponse>> create(@Valid @RequestBody EmployeeAssignmentRequest request,
                                                                            @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                            HttpServletRequest httpRequest) {
        EmployeeAssignmentResponse created = assignmentService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Employee assigned successfully", created));
    }

    @PostMapping("/api/employee-assignments/bulk")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGN')")
    public ResponseEntity<ApiResponse<BulkAssignmentResult>> bulkAssign(@Valid @RequestBody BulkEmployeeAssignmentRequest request,
                                                                          @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                          HttpServletRequest httpRequest) {
        BulkAssignmentResult result = assignmentService.bulkAssign(request, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                result.getAssigned() + " of " + result.getRequested() + " employees assigned successfully", result));
    }

    @GetMapping("/api/employee-assignments/active")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGNMENT_READ')")
    public ResponseEntity<ApiResponse<List<EmployeeAssignmentResponse>>> findAllActive() {
        return ResponseEntity.ok(ApiResponse.success("OK", assignmentService.findActiveForTenant()));
    }

    @PostMapping("/api/employee-assignments/bulk-end")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGN')")
    public ResponseEntity<ApiResponse<BulkEndResult>> bulkEnd(@Valid @RequestBody BulkEndAssignmentRequest request,
                                                                @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                HttpServletRequest httpRequest) {
        BulkEndResult result = assignmentService.bulkEnd(request, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                result.getEnded() + " of " + result.getRequested() + " assignments ended", result));
    }

    @PostMapping("/api/employee-assignments/{id}/end")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGN')")
    public ResponseEntity<ApiResponse<EmployeeAssignmentResponse>> end(@PathVariable Long id,
                                                                         @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                         HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Assignment ended", assignmentService.end(id, principal.getId(), httpRequest)));
    }

    @PostMapping("/api/employees/{employeeId}/transfer")
    @PreAuthorize("hasAuthority('EMPLOYEE_TRANSFER')")
    public ResponseEntity<ApiResponse<EmployeeAssignmentResponse>> transfer(@PathVariable Long employeeId,
                                                                              @Valid @RequestBody TransferEmployeeRequest request,
                                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Employee transferred successfully",
                assignmentService.transfer(employeeId, request, principal.getId(), httpRequest)));
    }

    @GetMapping("/api/employees/{employeeId}/assignments")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGNMENT_READ')")
    public ResponseEntity<ApiResponse<List<EmployeeAssignmentResponse>>> byEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("OK", assignmentService.findByEmployee(employeeId)));
    }

    @GetMapping("/api/sites/{siteId}/employees")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGNMENT_READ')")
    public ResponseEntity<ApiResponse<List<EmployeeAssignmentResponse>>> bySite(@PathVariable Long siteId) {
        return ResponseEntity.ok(ApiResponse.success("OK", assignmentService.findActiveBySite(siteId)));
    }
}
