package com.example.application.employee_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.employee_module.dto.*;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.employee_module.service.EmployeeService;
import com.example.application.employee_module.service.EmployeeBulkImportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeBulkImportService employeeBulkImportService;

    public EmployeeController(EmployeeService employeeService, EmployeeBulkImportService employeeBulkImportService) {
        this.employeeService = employeeService;
        this.employeeBulkImportService = employeeBulkImportService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean loginEnabled,
            @RequestParam(required = false) Long clientCompanyId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                employeeService.search(search, status, department, loginEnabled, clientCompanyId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", employeeService.findById(id)));
    }

    /** Preview of the code the Add form should show (disabled) before the user even submits. */
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> nextCode() {
        return ResponseEntity.ok(ApiResponse.success("OK", java.util.Map.of("code", employeeService.previewNextCode())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request,
                                                                  @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                  HttpServletRequest httpRequest) {
        EmployeeResponse created = employeeService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Employee created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody EmployeeUpdateRequest request,
                                                                  @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully",
                employeeService.update(id, request, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('EMPLOYEE_ACTIVATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> activate(@PathVariable Long id,
                                                                    @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Employee activated successfully",
                employeeService.activate(id, principal.getId(), httpRequest)));
    }

    /** For an ex-employee coming back - reactivates this SAME record (keeping all their history) with a freshly generated employeeCode. */
    @PutMapping("/{id}/rejoin")
    @PreAuthorize("hasAuthority('EMPLOYEE_ACTIVATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> rejoin(@PathVariable Long id,
                                                                  @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Employee rejoined with a new employee code",
                employeeService.rejoin(id, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('EMPLOYEE_DEACTIVATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> deactivate(@PathVariable Long id,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                      HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully",
                employeeService.deactivate(id, principal.getId(), httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_DELETE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> softDelete(@PathVariable Long id,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                      HttpServletRequest httpRequest) {
        // Soft-delete only, per spec section 38 - deactivation, never physical removal.
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully",
                employeeService.deactivate(id, principal.getId(), httpRequest)));
    }

    @PostMapping("/{id}/enable-login")
    @PreAuthorize("hasAuthority('EMPLOYEE_ENABLE_LOGIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> enableLogin(@PathVariable Long id,
                                                                       @Valid @RequestBody EnableLoginRequest request,
                                                                       @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                       HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Login access enabled successfully",
                employeeService.enableLogin(id, request, principal.getId(), httpRequest)));
    }

    @PostMapping("/{id}/disable-login")
    @PreAuthorize("hasAuthority('EMPLOYEE_DISABLE_LOGIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> disableLogin(@PathVariable Long id,
                                                                        @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Login access disabled successfully",
                employeeService.disableLogin(id, principal.getId(), httpRequest)));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGN_ROLE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> assignRole(@PathVariable Long id,
                                                                      @Valid @RequestBody AssignRoleRequest request,
                                                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                      HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully",
                employeeService.assignRole(id, request, principal.getId(), httpRequest)));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('EMPLOYEE_RESET_PASSWORD')")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@PathVariable Long id,
                                                                            @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                            HttpServletRequest httpRequest) {
        String tempPassword = employeeService.resetPassword(id, principal.getId(), httpRequest);
        // Returned exactly once, here, and never logged or persisted in plain text.
        return ResponseEntity.ok(ApiResponse.success(
                "Temporary password issued. It will not be shown again.",
                Map.of("temporaryPassword", tempPassword)));
    }

    /** A downloadable starting point with the exact expected headers plus one example row. */
    @GetMapping("/bulk-import/template")
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] workbook = employeeBulkImportService.generateTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employee-import-template.xlsx\"")
                .body(workbook);
    }

    /** Best-effort, row-by-row import (see EmployeeBulkImportService javadoc) - one bad row never blocks the good ones. */
    @PostMapping("/bulk-import")
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<EmployeeBulkImportResult>> bulkImport(@RequestParam("file") MultipartFile file,
                                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                                              HttpServletRequest httpRequest) {
        EmployeeBulkImportResult result = employeeBulkImportService.importFromExcel(file, principal.getId(), httpRequest);
        String message = result.getSuccessCount() + " of " + result.getTotalRows() + " employee(s) imported successfully";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}
