package com.example.application.payslip_module.service;

import com.example.application.client_company_module.entity.ClientCompany;
import com.example.application.client_company_module.repository.ClientCompanyRepository;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.payroll_module.entity.PayrollRun;
import com.example.application.payroll_module.entity.PayrollRunEmployee;
import com.example.application.payroll_module.repository.PayrollRunEmployeeRepository;
import com.example.application.payroll_module.repository.PayrollRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Individual payslip PDFs - one employee, one month, read entirely from an already-calculated
 * PayrollRunEmployee row (never recalculates anything, same "reports read, they don't compute"
 * rule as MonthlyAttendanceReportService/SalaryRegisterExportService).
 *
 * A payslip is only issued once payroll for that month is APPROVED or PAID - not while still
 * DRAFT/CALCULATED, since those can still be edited/recalculated and handing out a "payslip" for
 * numbers that might still change would be misleading.
 *
 * Two entry points:
 *   - generatePayslip(tenantId, employeeId, year, month): admin picks any employee in their
 *     tenant (PayrollRunController gates this with PAYROLL_RUN_READ).
 *   - generateMyPayslip(userId, year, month): self-service - resolves the employee from the
 *     CURRENTLY LOGGED IN user via Employee.userId, so an employee can only ever reach their own
 *     payslip, never anyone else's, with no extra permission needed beyond being logged in.
 */
@Service
public class PayslipService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunEmployeeRepository payrollRunEmployeeRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientCompanyRepository clientCompanyRepository;
    private final TenantContextService tenantContext;

    public PayslipService(PayrollRunRepository payrollRunRepository,
                           PayrollRunEmployeeRepository payrollRunEmployeeRepository,
                           EmployeeRepository employeeRepository,
                           ClientCompanyRepository clientCompanyRepository,
                           TenantContextService tenantContext) {
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRunEmployeeRepository = payrollRunEmployeeRepository;
        this.employeeRepository = employeeRepository;
        this.clientCompanyRepository = clientCompanyRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public byte[] generatePayslip(Long employeeId, int year, int month) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollRunEmployee pre = findApprovedOrPaidRunEmployee(tenantId, employeeId, year, month);
        ClientCompany company = clientCompanyRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client company not found: " + tenantId));
        return render(pre, company, year, month);
    }

    /** Self-service - the employee viewing/downloading their OWN payslip, resolved from their own login. */
    @Transactional(readOnly = true)
    public byte[] generateMyPayslip(Long userId, int year, int month) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No employee record is linked to this login."));
        PayrollRunEmployee pre = findApprovedOrPaidRunEmployee(employee.getClientCompanyId(), employee.getId(), year, month);
        ClientCompany company = clientCompanyRepository.findById(employee.getClientCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Client company not found: " + employee.getClientCompanyId()));
        return render(pre, company, year, month);
    }

    private PayrollRunEmployee findApprovedOrPaidRunEmployee(Long tenantId, Long employeeId, int year, int month) {
        List<PayrollRun> runs = payrollRunRepository.findAllByClientCompanyIdAndYearAndMonthAndStatusNot(tenantId, year, month, "CANCELLED");
        if (runs.isEmpty()) {
            throw new ResourceNotFoundException("No payroll run exists for " + month + "/" + year + ".");
        }
        PayrollRun run = runs.stream().max(Comparator.comparing(PayrollRun::getCreatedAt)).orElseThrow();
        if (!"APPROVED".equals(run.getStatus()) && !"PAID".equals(run.getStatus())) {
            throw new BadRequestException("Payroll for " + month + "/" + year
                    + " has not been approved yet - a payslip isn't issued until then (currently: " + run.getStatus() + ").");
        }
        return payrollRunEmployeeRepository.findByPayrollRunIdAndEmployeeId(run.getId(), employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No payslip found for this employee for " + month + "/" + year + "."));
    }

    private byte[] render(PayrollRunEmployee pre, ClientCompany company, int year, int month) {
        String monthLabel = Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + year;
        String html = buildHtml(pre, company, monthLabel);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate the payslip PDF", e);
        }
    }

    private String buildHtml(PayrollRunEmployee r, ClientCompany company, String monthLabel) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>")
          .append("body { font-family: Helvetica, Arial, sans-serif; font-size: 11px; color: #1e293b; }")
          .append(".header { text-align: center; margin-bottom: 14px; }")
          .append(".header h1 { font-size: 17px; margin: 0; }")
          .append(".header p { margin: 2px 0; color: #555; }")
          .append(".meta-table { width: 100%; margin-bottom: 14px; border-collapse: collapse; }")
          .append(".meta-table td { padding: 3px 6px; font-size: 11px; }")
          .append(".meta-table td.label { color: #64748b; width: 25%; }")
          .append("table.amounts { width: 100%; border-collapse: collapse; margin-bottom: 10px; }")
          .append("table.amounts th { background: #1d4ed8; color: #fff; text-align: left; padding: 5px 8px; font-size: 10.5px; }")
          .append("table.amounts td { border-bottom: 1px solid #e2e8f0; padding: 5px 8px; }")
          .append("td.num { text-align: right; }")
          .append(".net-pay-box { margin-top: 14px; padding: 10px 14px; background: #eff4ff; border: 1px solid #1d4ed8; border-radius: 4px; text-align: right; }")
          .append(".net-pay-box .label { font-size: 11px; color: #555; }")
          .append(".net-pay-box .amount { font-size: 18px; font-weight: bold; color: #1d4ed8; }")
          .append(".footer-note { margin-top: 20px; font-size: 9.5px; color: #94a3b8; text-align: center; }")
          .append("</style></head><body>");

        sb.append("<div class=\"header\">")
          .append("<h1>").append(escape(company.getCompanyName())).append("</h1>")
          .append("<p>").append(escape(nullToEmpty(company.getAddress()))).append("</p>")
          .append("<p><strong>Payslip for ").append(escape(monthLabel)).append("</strong></p>")
          .append("</div>");

        sb.append("<table class=\"meta-table\"><tr>")
          .append("<td class=\"label\">Employee</td><td>").append(escape(r.getEmployeeCode())).append(" &#183; ").append(escape(r.getEmployeeName())).append("</td>")
          .append("<td class=\"label\">Department</td><td>").append(escape(nullToEmpty(r.getDepartment()))).append("</td>")
          .append("</tr><tr>")
          .append("<td class=\"label\">Designation</td><td>").append(escape(nullToEmpty(r.getDesignation()))).append("</td>")
          .append("<td class=\"label\">Site</td><td>").append(escape(nullToEmpty(r.getSiteName()))).append("</td>")
          .append("</tr><tr>")
          .append("<td class=\"label\">Payable Days</td><td>").append(r.getPayableDays()).append(" / ").append(r.getTotalCalendarDays()).append("</td>")
          .append("<td class=\"label\">Salary Structure</td><td>").append(escape(nullToEmpty(r.getSalaryStructureName()))).append("</td>")
          .append("</tr></table>");

        sb.append("<table class=\"amounts\"><tr><th>Earnings</th><th style=\"text-align:right;\">Amount (&#8377;)</th></tr>");
        appendRow(sb, "Basic Salary", r.getBasicSalary());
        appendRow(sb, "Dearness Allowance (DA)", r.getDa());
        if (r.getAllowance() != null && r.getAllowance().compareTo(BigDecimal.ZERO) != 0) {
            appendRow(sb, "Allowance", r.getAllowance());
        }
        appendRow(sb, "Gross Salary", r.getGrossSalary());
        sb.append("</table>");

        sb.append("<table class=\"amounts\"><tr><th>Deductions</th><th style=\"text-align:right;\">Amount (&#8377;)</th></tr>");
        appendRow(sb, "Provident Fund (PF)", r.getEpfEmployee());
        appendRow(sb, "ESI", r.getEsiEmployee());
        appendRow(sb, "Professional Tax", r.getProfessionalTax());
        if (r.getOtherManualDeduction() != null && r.getOtherManualDeduction().compareTo(BigDecimal.ZERO) != 0) {
            appendRow(sb, "Other Deduction", r.getOtherManualDeduction());
        }
        if (r.getAdvanceRecovery() != null && r.getAdvanceRecovery().compareTo(BigDecimal.ZERO) != 0) {
            appendRow(sb, "Advance Recovery", r.getAdvanceRecovery());
        }
        appendRow(sb, "Total Deductions", r.getTotalDeductions());
        sb.append("</table>");

        sb.append("<div class=\"net-pay-box\"><span class=\"label\">Net Pay: </span><span class=\"amount\">&#8377;").append(r.getNetPay()).append("</span></div>");

        sb.append("<p class=\"footer-note\">This is a system-generated payslip and does not require a signature.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendRow(StringBuilder sb, String label, BigDecimal amount) {
        sb.append("<tr><td>").append(escape(label)).append("</td><td class=\"num\">")
          .append(amount == null ? "0.00" : amount).append("</td></tr>");
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
