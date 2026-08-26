package com.example.application.attendance_module.controller;

import com.example.application.attendance_module.dto.LeaveAdjustmentRequest;
import com.example.application.attendance_module.dto.MonthlyAttendanceReportResponse;
import com.example.application.attendance_module.service.MonthlyAttendanceReportService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bulk (all-employees-at-once) monthly ATTENDANCE report - gated by
 * MONTHLY_PAYMENT_REPORT_EXPORT - granted only to CLIENT_ADMIN (and
 * SUPER_ADMIN by the usual "SUPER_ADMIN gets every permission" convention,
 * though the report is inherently tenant-scoped so SUPER_ADMIN would need to
 * be acting within a tenant to actually call it - see TenantContextService).
 *
 * Architecture refactor Phase 4: this controller/service contains NO
 * payroll data or calculation - Gross/PF/ESI/PT/Advance Recovery/Net Pay
 * all live exclusively under /api/payroll/runs (see payroll_module).
 * Viewing this report (GET, GET .../download) never writes anything -
 * see MonthlyAttendanceReportService's use of EmployeePaidLeaveService
 * .previewMonth(). PUT .../leave-adjustment is a deliberate, explicit
 * exception: an admin correcting one employee's recorded paid-leave usage
 * for a month is a Leave-domain action, not a passive report view, so it's
 * allowed to write via the Paid Leave module's own manual-override methods.
 */
@RestController
@RequestMapping("/api/attendance/monthly-report")
public class MonthlyAttendanceReportController {

    private final MonthlyAttendanceReportService reportService;

    public MonthlyAttendanceReportController(MonthlyAttendanceReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MONTHLY_PAYMENT_REPORT_EXPORT')")
    public ResponseEntity<ApiResponse<MonthlyAttendanceReportResponse>> preview(
            @RequestParam int year, @RequestParam int month,
            @RequestParam(required = false) List<Long> siteIds) {
        return ResponseEntity.ok(ApiResponse.success("OK", reportService.computeReport(year, month, siteIds)));
    }

    @GetMapping("/download")
    @PreAuthorize("hasAuthority('MONTHLY_PAYMENT_REPORT_EXPORT')")
    public ResponseEntity<byte[]> download(@RequestParam int year, @RequestParam int month,
                                            @RequestParam(defaultValue = "xlsx") String format,
                                            @RequestParam(required = false) List<Long> siteIds) {
        byte[] file;
        String extension;
        MediaType mediaType;
        switch (format.toLowerCase()) {
            case "pdf" -> {
                file = reportService.generatePdf(year, month, siteIds);
                extension = "pdf";
                mediaType = MediaType.APPLICATION_PDF;
            }
            case "xlsx" -> {
                file = reportService.generateExcel(year, month, siteIds);
                extension = "xlsx";
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }
            default -> throw new BadRequestException("format must be 'xlsx' or 'pdf'");
        }
        String filename = String.format("Monthly-Attendance-Report-%04d-%02d.%s", year, month, extension);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(file);
    }

    /** paidDaysUsed = null clears the manual adjustment, reverting to the auto-calculated figure. */
    @PutMapping("/leave-adjustment")
    @PreAuthorize("hasAuthority('MONTHLY_PAYMENT_REPORT_EXPORT')")
    public ResponseEntity<ApiResponse<Void>> adjustPaidLeave(@Valid @RequestBody LeaveAdjustmentRequest request,
                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                              HttpServletRequest httpRequest) {
        if (request.getPaidDaysUsed() == null) {
            reportService.clearPaidLeaveAdjustment(request.getEmployeeId(), request.getYear(), request.getMonth(),
                    principal.getId(), httpRequest);
        } else {
            reportService.adjustPaidLeave(request.getEmployeeId(), request.getYear(), request.getMonth(),
                    request.getPaidDaysUsed(), principal.getId(), httpRequest);
        }
        return ResponseEntity.ok(ApiResponse.success("Paid leave updated"));
    }
}
