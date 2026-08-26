package com.example.application.salary_structure_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.salary_structure_module.dto.AssignSalaryStructureRequest;
import com.example.application.salary_structure_module.dto.EmployeeSalaryStructureResponse;
import com.example.application.salary_structure_module.service.EmployeeSalaryStructureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Matches spec section 90: POST /api/employees/{id}/salary-structure, GET /api/employees/{id}/salary-history. */
@RestController
@RequestMapping("/api/employees/{employeeId}")
public class EmployeeSalaryStructureController {

    private final EmployeeSalaryStructureService assignmentService;

    public EmployeeSalaryStructureController(EmployeeSalaryStructureService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/salary-structure")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_READ')")
    public ResponseEntity<ApiResponse<EmployeeSalaryStructureResponse>> current(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("OK", assignmentService.findCurrent(employeeId)));
    }

    @PostMapping("/salary-structure")
    @PreAuthorize("hasAuthority('SALARY_ASSIGN')")
    public ResponseEntity<ApiResponse<EmployeeSalaryStructureResponse>> assign(
            @PathVariable Long employeeId,
            @Valid @RequestBody AssignSalaryStructureRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        EmployeeSalaryStructureResponse assigned = assignmentService.assign(employeeId, request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Salary structure assigned successfully", assigned));
    }

    @GetMapping("/salary-history")
    @PreAuthorize("hasAuthority('SALARY_STRUCTURE_READ')")
    public ResponseEntity<ApiResponse<List<EmployeeSalaryStructureResponse>>> history(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("OK", assignmentService.findHistory(employeeId)));
    }
}
