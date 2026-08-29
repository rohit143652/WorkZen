package com.example.application.payslip_module.controller;

import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.payslip_module.service.PayslipService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Individual payslip PDFs - deliberately its own controller, separate from PayrollRunController
 * (which is CLIENT_ADMIN-only throughout): the "me" endpoint below must be reachable by ANY
 * logged-in employee, not just admins, so it can't live under a controller whose whole class doc
 * states the opposite invariant.
 */
@RestController
@RequestMapping("/api/payroll/payslip")
public class PayslipController {

    private final PayslipService payslipService;

    public PayslipController(PayslipService payslipService) {
        this.payslipService = payslipService;
    }

    /**
     * Self-service - gated on PAYSLIP_SELF_VIEW (granted to every role by default - see V84
     * migration - adjustable per-role at any time from Roles -> Edit like any other permission),
     * but PayslipService additionally resolves the employee from THIS user's own login
     * (Employee.userId) regardless - there is no way to pass in a different employeeId here, so
     * nobody can ever reach anyone else's payslip through this path even if they somehow held
     * the permission without being an actual employee.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('PAYSLIP_SELF_VIEW')")
    public ResponseEntity<byte[]> myPayslip(@RequestParam int year, @RequestParam int month,
                                             @AuthenticationPrincipal CustomUserPrincipal principal) {
        byte[] pdf = payslipService.generateMyPayslip(principal.getId(), year, month);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Payslip-" + month + "-" + year + ".pdf\"")
                .body(pdf);
    }

    /** Admin path - Client Admin (or anyone else holding PAYROLL_RUN_READ) picks any employee in their own tenant. */
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('PAYROLL_RUN_READ')")
    public ResponseEntity<byte[]> employeePayslip(@PathVariable Long employeeId, @RequestParam int year, @RequestParam int month) {
        byte[] pdf = payslipService.generatePayslip(employeeId, year, month);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Payslip-" + employeeId + "-" + month + "-" + year + ".pdf\"")
                .body(pdf);
    }
}
