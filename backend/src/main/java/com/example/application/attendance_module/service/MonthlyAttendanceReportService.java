package com.example.application.attendance_module.service;

import com.example.application.attendance_module.dto.MonthlyAttendanceReportResponse;
import com.example.application.attendance_module.dto.MonthlyAttendanceReportRow;
import com.example.application.attendance_module.entity.Attendance;
import com.example.application.attendance_module.repository.AttendanceRepository;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.entity.EmployeeSiteAssignment;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.leave_module.service.EmployeePaidLeaveService;
import com.example.application.leave_module.service.LeavePolicyResolver;
import com.example.application.payroll_module.dto.EmployeePayrollInputs;
import com.example.application.payroll_module.service.PayrollInputResolver;
import com.example.application.salary_structure_module.dto.SalaryStructureResponse;
import com.example.application.site_module.entity.Site;
import com.example.application.site_module.repository.SiteRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates the Monthly Attendance Report: one row per active employee for
 * the requested month, with their attendance breakdown (present/half-day/
 * on-leave/absent) and how much of their On Leave is paid vs unpaid.
 *
 * ARCHITECTURE REFACTOR PHASE 4: this service contains NO money and NO
 * payroll math. It never calls PayrollCalculationService, PayrollSettings,
 * or EmployeePayrollAdjustment - those live exclusively under
 * payroll_module.PayrollRunService / /api/payroll/runs. Gross Salary, PF,
 * ESI, PT, Advance Recovery, and Net Pay are not this service's concern at
 * all; if you need those, look at a persisted PayrollRun's
 * PayrollRunEmployee rows, not this report.
 *
 * READ-ONLY GUARANTEE: computeReport() (and therefore the JSON preview and
 * both Excel/PDF downloads, which all call it) uses
 * payroll_module.PayrollInputResolver.previewEmployeeInputs() - the
 * read-only sibling of the method PayrollRunService.calculateRun() uses -
 * so simply viewing this report NEVER creates or modifies a Paid Leave
 * balance row, an AdvanceRecoveryTransaction, or any payroll data. The one
 * deliberate exception is the explicit, admin-initiated PUT
 * .../leave-adjustment action below, which is a Leave-domain correction,
 * not a passive report view - see EmployeePaidLeaveService's own
 * setManualUsage()/clearManualUsage(), which this only delegates to.
 *
 * Paid vs unpaid leave: Attendance's ON_LEAVE status is a FACT, not a
 * business decision - it does NOT by itself mean "paid" or "unpaid".
 * leave_module.EmployeePaidLeaveService decides how many of an employee's
 * ON_LEAVE days this month are covered by their available paid-leave
 * balance; whatever's left over is unpaid. This service only surfaces
 * that leave-module answer, it never hardcodes the interpretation itself.
 */
@Service
public class MonthlyAttendanceReportService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeSiteAssignmentRepository siteAssignmentRepository;
    private final SiteRepository siteRepository;
    private final PayrollInputResolver payrollInputResolver;
    private final EmployeePaidLeaveService paidLeaveService;
    private final LeavePolicyResolver leavePolicyResolver;
    private final TenantContextService tenantContext;

    public MonthlyAttendanceReportService(EmployeeRepository employeeRepository,
                                           AttendanceRepository attendanceRepository,
                                           EmployeeSiteAssignmentRepository siteAssignmentRepository,
                                           SiteRepository siteRepository,
                                           PayrollInputResolver payrollInputResolver,
                                           EmployeePaidLeaveService paidLeaveService,
                                           LeavePolicyResolver leavePolicyResolver,
                                           TenantContextService tenantContext) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.siteAssignmentRepository = siteAssignmentRepository;
        this.siteRepository = siteRepository;
        this.payrollInputResolver = payrollInputResolver;
        this.paidLeaveService = paidLeaveService;
        this.leavePolicyResolver = leavePolicyResolver;
        this.tenantContext = tenantContext;
    }

    /** Read-only: previewEmployeeInputs() below never writes to Leave, Salary Structure, or Payroll data. */
    @Transactional(readOnly = true)
    public MonthlyAttendanceReportResponse computeReport(int year, int month, List<Long> siteIds) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        Long tenantId = tenantContext.requireCurrentTenantId();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        int daysInMonth = yearMonth.lengthOfMonth();

        List<Employee> employees = employeeRepository.findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(tenantId, "ACTIVE");

        Map<Long, List<Attendance>> attendanceByEmployee = attendanceRepository
                .findAllByClientCompanyIdAndAttendanceDateBetweenOrderByEmployeeIdAscAttendanceDateAsc(tenantId, monthStart, monthEnd)
                .stream().collect(Collectors.groupingBy(Attendance::getEmployeeId));

        Map<Long, String> siteNameById = siteRepository.findAllByClientCompanyId(tenantId).stream()
                .collect(Collectors.toMap(Site::getId, Site::getSiteName));

        Map<Long, EmployeeSiteAssignment> currentSiteByEmployee = new HashMap<>();
        for (EmployeeSiteAssignment a : siteAssignmentRepository.findAllByClientCompanyIdAndStatus(tenantId, "ACTIVE")) {
            currentSiteByEmployee.merge(a.getEmployeeId(), a,
                    (existing, candidate) -> (candidate.isPrimary() && !existing.isPrimary()) ? candidate : existing);
        }

        // Site filter: empty/null siteIds = all sites (default). Otherwise, only employees
        // currently assigned to one of the selected sites are included - an employee with no
        // site assignment at all is only shown under "All Sites", never under a specific one.
        if (siteIds != null && !siteIds.isEmpty()) {
            Set<Long> siteIdSet = new HashSet<>(siteIds);
            employees = employees.stream()
                    .filter(e -> {
                        EmployeeSiteAssignment a = currentSiteByEmployee.get(e.getId());
                        return a != null && siteIdSet.contains(a.getSiteId());
                    })
                    .toList();
        }

        Map<Long, SalaryStructureResponse> structureCache = new HashMap<>();

        List<MonthlyAttendanceReportRow> rows = new ArrayList<>();
        for (Employee e : employees) {
            List<Attendance> marked = attendanceByEmployee.getOrDefault(e.getId(), List.of());
            EmployeePayrollInputs in = payrollInputResolver.previewEmployeeInputs(
                    tenantId, e, year, month, monthEnd, daysInMonth, marked, structureCache);

            EmployeeSiteAssignment assignment = currentSiteByEmployee.get(e.getId());
            String siteName = assignment != null ? siteNameById.getOrDefault(assignment.getSiteId(), "Unknown Site") : "Unassigned";

            MonthlyAttendanceReportRow row = new MonthlyAttendanceReportRow();
            row.setEmployeeId(e.getId());
            row.setEmployeeCode(e.getEmployeeCode());
            row.setEmployeeName(String.join(" ", Arrays.asList(e.getFirstName(), nullToEmpty(e.getMiddleName()), e.getLastName()))
                    .replaceAll("\\s+", " ").trim());
            row.setDepartment(e.getDepartment());
            row.setDesignation(e.getDesignation());
            row.setCurrentSite(siteName);
            row.setPresentDays(in.getPresentDays());
            row.setHalfDays(in.getHalfDays());
            row.setOnLeaveDays(in.getOnLeaveDays());
            row.setAbsentDays(in.getAbsentDays());
            row.setPaidLeaveDays(in.getPaidLeaveDays());
            row.setUnpaidLeaveDays(in.getUnpaidLeaveDays());
            row.setPayableDays(in.getPayableDays());
            row.setLeaveBalanceOpening(in.getLeaveBalanceOpening());
            row.setLeaveBalanceClosing(in.getLeaveBalanceClosing());
            row.setManualLeaveOverride(in.isManualLeaveOverride());

            rows.add(row);
        }

        String monthLabel = yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + yearMonth.getYear();
        return new MonthlyAttendanceReportResponse(year, month, monthLabel, daysInMonth, rows);
    }

    /**
     * Direct edit from the report table: sets a specific paid-leave figure for one
     * employee+month (bounded by that month's actual ON_LEAVE days). This is a deliberate,
     * explicit Leave-domain write, delegated to leave_module's own EmployeePaidLeaveService -
     * MonthlyAttendanceReportService never touches Leave repositories directly (spec section 15).
     */
    @Transactional
    public void adjustPaidLeave(Long employeeId, int year, int month, BigDecimal paidDaysUsed,
                                 Long actorId, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        YearMonth ym = YearMonth.of(year, month);
        long onLeaveDays = attendanceRepository.countByClientCompanyIdAndEmployeeIdAndAttendanceDateBetweenAndStatus(
                tenantId, employeeId, ym.atDay(1), ym.atEndOfMonth(), "ON_LEAVE");
        if (paidDaysUsed.signum() < 0 || paidDaysUsed.compareTo(BigDecimal.valueOf(onLeaveDays)) > 0) {
            throw new BadRequestException("Paid leave days must be between 0 and the " + onLeaveDays + " on-leave day(s) marked this month");
        }
        paidLeaveService.setManualUsage(tenantId, employeeId, year, month, paidDaysUsed, actorId, httpRequest);
    }

    /** Reverts one employee+month back to the auto-calculated paid-leave figure. */
    @Transactional
    public void clearPaidLeaveAdjustment(Long employeeId, int year, int month,
                                          Long actorId, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        paidLeaveService.clearManualUsage(tenantId, employeeId, year, month, actorId, httpRequest);
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    @Transactional(readOnly = true)
    public byte[] generateExcel(int year, int month, List<Long> siteIds) {
        MonthlyAttendanceReportResponse report = computeReport(year, month, siteIds);
        boolean leaveEnabled = isLeaveEnabledForMonth(year, month);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(report.getMonthLabel());

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            String[] headers = buildHeaders(leaveEnabled);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0, 1);

            int rowIndex = 1;
            for (MonthlyAttendanceReportRow r : report.getRows()) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                row.createCell(col++).setCellValue(r.getEmployeeCode());
                row.createCell(col++).setCellValue(r.getEmployeeName());
                row.createCell(col++).setCellValue(nullToEmpty(r.getDepartment()));
                row.createCell(col++).setCellValue(nullToEmpty(r.getDesignation()));
                row.createCell(col++).setCellValue(r.getCurrentSite());
                row.createCell(col++).setCellValue(r.getPresentDays());
                row.createCell(col++).setCellValue(r.getHalfDays());
                row.createCell(col++).setCellValue(r.getOnLeaveDays());
                row.createCell(col++).setCellValue(r.getAbsentDays());

                if (leaveEnabled) {
                    Cell paidLeaveCell = row.createCell(col++);
                    paidLeaveCell.setCellValue(r.getPaidLeaveDays().doubleValue());
                    paidLeaveCell.setCellStyle(numberStyle);

                    Cell unpaidLeaveCell = row.createCell(col++);
                    unpaidLeaveCell.setCellValue(r.getUnpaidLeaveDays().doubleValue());
                    unpaidLeaveCell.setCellStyle(numberStyle);

                    Cell leaveBalanceCell = row.createCell(col++);
                    leaveBalanceCell.setCellValue(r.getLeaveBalanceClosing() == null ? 0 : r.getLeaveBalanceClosing().doubleValue());
                    leaveBalanceCell.setCellStyle(numberStyle);
                }

                Cell payableCell = row.createCell(col);
                payableCell.setCellValue(r.getPayableDays().doubleValue());
                payableCell.setCellStyle(numberStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate the monthly attendance report workbook", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(int year, int month, List<Long> siteIds) {
        MonthlyAttendanceReportResponse report = computeReport(year, month, siteIds);
        boolean leaveEnabled = isLeaveEnabledForMonth(year, month);
        String html = buildReportHtml(report, leaveEnabled);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate the monthly attendance report PDF", e);
        }
    }

    /** Whether Paid Leave was switched on for this client for this specific month - governs whether the Paid/Unpaid Leave and Leave Balance columns are included in exports at all, matching the on-screen preview. */
    private boolean isLeaveEnabledForMonth(int year, int month) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return leavePolicyResolver.resolve(tenantId, year, month).isEnabled();
    }

    private static final String[] BASE_HEADERS = {
            "Employee Code", "Employee Name", "Department", "Designation", "Current Site",
            "Present", "Half Day", "On Leave", "Absent"
    };
    private static final String[] LEAVE_HEADERS = { "Paid Leave", "Unpaid Leave", "Leave Balance" };
    private static final String[] TAIL_HEADERS = { "Payable Days" };

    private String[] buildHeaders(boolean leaveEnabled) {
        List<String> headers = new ArrayList<>(Arrays.asList(BASE_HEADERS));
        if (leaveEnabled) {
            headers.addAll(Arrays.asList(LEAVE_HEADERS));
        }
        headers.addAll(Arrays.asList(TAIL_HEADERS));
        return headers.toArray(new String[0]);
    }

    private String buildReportHtml(MonthlyAttendanceReportResponse report, boolean leaveEnabled) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>")
          .append("body { font-family: Helvetica, Arial, sans-serif; font-size: 10px; }")
          .append("h1 { font-size: 15px; margin-bottom: 2px; }")
          .append("p.sub { font-size: 10px; color: #555; margin-top: 0; }")
          .append("table { border-collapse: collapse; width: 100%; margin-top: 10px; }")
          .append("th, td { border: 1px solid #999; padding: 3px 6px; text-align: left; }")
          .append("th { background-color: #1d4ed8; color: #fff; font-size: 9px; }")
          .append("td.num { text-align: right; }")
          .append("</style></head><body>");
        sb.append("<h1>Monthly Attendance Report</h1>");
        sb.append("<p class=\"sub\">").append(escape(report.getMonthLabel())).append("</p>");
        sb.append("<table><thead><tr>");
        for (String h : buildHeaders(leaveEnabled)) {
            sb.append("<th>").append(escape(h)).append("</th>");
        }
        sb.append("</tr></thead><tbody>");
        for (MonthlyAttendanceReportRow r : report.getRows()) {
            sb.append("<tr>")
              .append("<td>").append(escape(r.getEmployeeCode())).append("</td>")
              .append("<td>").append(escape(r.getEmployeeName())).append("</td>")
              .append("<td>").append(escape(r.getDepartment())).append("</td>")
              .append("<td>").append(escape(r.getDesignation())).append("</td>")
              .append("<td>").append(escape(r.getCurrentSite())).append("</td>")
              .append("<td class=\"num\">").append(r.getPresentDays()).append("</td>")
              .append("<td class=\"num\">").append(r.getHalfDays()).append("</td>")
              .append("<td class=\"num\">").append(r.getOnLeaveDays()).append("</td>")
              .append("<td class=\"num\">").append(r.getAbsentDays()).append("</td>");
            if (leaveEnabled) {
                sb.append("<td class=\"num\">").append(r.getPaidLeaveDays()).append("</td>")
                  .append("<td class=\"num\">").append(r.getUnpaidLeaveDays()).append("</td>")
                  .append("<td class=\"num\">").append(r.getLeaveBalanceClosing() == null ? "-" : r.getLeaveBalanceClosing()).append("</td>");
            }
            sb.append("<td class=\"num\"><strong>").append(r.getPayableDays()).append("</strong></td>")
              .append("</tr>");
        }
        sb.append("</tbody></table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
