package com.example.application.audit_module.controller;

import com.example.application.audit_module.dto.AuditLogResponse;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_READ')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> findAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", auditService.findAll(pageable)));
    }
}
