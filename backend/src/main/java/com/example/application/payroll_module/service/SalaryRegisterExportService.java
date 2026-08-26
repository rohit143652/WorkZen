package com.example.application.payroll_module.service;

import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.payroll_module.entity.PayrollRun;
import com.example.application.payroll_module.entity.PayrollRunEmployee;
import com.example.application.payroll_module.repository.PayrollRunEmployeeRepository;
import com.example.application.payroll_module.repository.PayrollRunRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Generates the classic "Salary Register" Excel layout (SR NO / EMP CODE /
 * EMPLOYEE'S NAME / BASIC / DA / ... / NET PAYMENT, with merged
 * "EMPLOYEE DED." and "EMPLOYER CONT." group headers) that predates this
 * project's PayrollRun architecture - kept as an additional export
 * alongside the standard Payroll Run Details view, for teams already used
 * to this specific spreadsheet shape.
 *
 * READ-ONLY, no new calculation: every figure comes directly from an
 * already-persisted PayrollRunEmployee row (see payroll_module.
 * PayrollCalculationService for how those were computed) - this class
 * only re-arranges existing numbers into a different visual layout. The
 * two exceptions are clearly-derived, not independently calculated,
 * figures:
 *   "HRA Subtotal" = Gross - Basic - DA (whatever the Salary Structure's
 *     non-Basic/DA earning components add up to, however they're actually
 *     configured - not a live 5% formula, so it never disagrees with Gross).
 *   "Monthly Wages Amt." = Gross - Total Deductions (i.e. Net Pay before
 *     Allowance - the same split the legacy template itself uses between
 *     "Monthly Wages" and "Net Payment").
 * "Working Days" reuses Total Calendar Days, since this project has no
 * separate working-day/weekly-off configuration (documented gap - see the
 * architecture audit).
 */
@Service
public class SalaryRegisterExportService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunEmployeeRepository payrollRunEmployeeRepository;
    private final TenantContextService tenantContext;

    public SalaryRegisterExportService(PayrollRunRepository payrollRunRepository,
                                        PayrollRunEmployeeRepository payrollRunEmployeeRepository,
                                        TenantContextService tenantContext) {
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRunEmployeeRepository = payrollRunEmployeeRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public byte[] generate(Long runId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollRun run = payrollRunRepository.findByIdAndClientCompanyId(runId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run " + runId + " not found"));
        if ("DRAFT".equals(run.getStatus())) {
            throw new BadRequestException("This payroll run has not been calculated yet - nothing to export");
        }
        List<PayrollRunEmployee> rows = payrollRunEmployeeRepository.findAllByPayrollRunIdOrderByEmployeeCodeAsc(runId);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Salary Register");

            CellStyle groupHeaderStyle = headerStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE);
            CellStyle headerStyle = headerStyle(workbook, IndexedColors.PALE_BLUE);
            CellStyle numberStyle = numberStyle(workbook);
            CellStyle highlightStyle = workbook.createCellStyle();
            highlightStyle.cloneStyleFrom(numberStyle);
            highlightStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            highlightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            writeHeader(sheet, groupHeaderStyle, headerStyle);

            BigDecimal[] totals = new BigDecimal[COLUMN_COUNT];
            for (int i = 0; i < COLUMN_COUNT; i++) totals[i] = BigDecimal.ZERO;

            int rowIndex = 2;
            int sr = 1;
            for (PayrollRunEmployee e : rows) {
                Row row = sheet.createRow(rowIndex++);
                BigDecimal[] values = rowValues(e);
                writeRow(row, sr++, e.getEmployeeCode(), e.getEmployeeName(), values, numberStyle, highlightStyle);
                for (int i = 0; i < COLUMN_COUNT; i++) {
                    totals[i] = totals[i].add(values[i]);
                }
            }

            Row totalRow = sheet.createRow(rowIndex);
            writeTotalRow(totalRow, totals, numberStyle, highlightStyle);

            for (int i = 0; i < TOTAL_COLUMNS; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.createFreezePane(3, 2);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate the salary register workbook", e);
        }
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private static final int COLUMN_COUNT = 23; // numeric columns only, after the 3 identity columns - must match COLUMN_LABELS.length - 3 and rowValues()'s array length
    private static final int TOTAL_COLUMNS = 3 + COLUMN_COUNT;

    private static final String[] GROUP_LABELS = { "EMPLOYEE DED.", "EMPLOYER CONT." };
    private static final String[] COLUMN_LABELS = {
            "SR NO.", "EMP CODE", "EMPLOYEE'S NAME",
            "BASIC\nSALARY", "DA", "TOTAL\nBASIC+DA", "MONTH\nDAYS", "WORKING\nDAYS", "PER\nDAY", "PRESENT\nDAYS", "LEAVE\nDAYS",
            "BASIC+DA\n(A)", "HRA\nSUBTOTAL", "TOTAL\nGross",
            "E.P.F.\n12%(A)", "ESI\n0.75%(B)", "E.P.F.\n13%(A)", "ESI\n3.25%(B)",
            "Total\nSalary", "P.T.", "ADVC. &\nUNIFORM", "ADVANCE\nRECOVERY", "TOTAL\nDEDUCT", "MONTHLY\nWAGES AMT.", "Allownce", "NET\nPAY-MENT"
    };

    private void writeHeader(Sheet sheet, CellStyle groupStyle, CellStyle headerStyle) {
        Row groupRow = sheet.createRow(0);
        Cell dedCell = groupRow.createCell(14); // E.P.F. 12% column
        dedCell.setCellValue(GROUP_LABELS[0]);
        dedCell.setCellStyle(groupStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 14, 15));

        Cell contCell = groupRow.createCell(16); // E.P.F. 13% column
        contCell.setCellValue(GROUP_LABELS[1]);
        contCell.setCellStyle(groupStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 16, 17));

        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < COLUMN_LABELS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(COLUMN_LABELS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /** Same column order as COLUMN_LABELS[3..], after the 3 identity columns. */
    private BigDecimal[] rowValues(PayrollRunEmployee e) {
        BigDecimal basic = nz(e.getBasicSalary());
        BigDecimal da = nz(e.getDa());
        BigDecimal basicPlusDa = basic.add(da);
        BigDecimal gross = nz(e.getGrossSalary());
        BigDecimal hra = gross.subtract(basicPlusDa).max(BigDecimal.ZERO);
        BigDecimal perDay = e.getTotalCalendarDays() > 0
                ? gross.divide(BigDecimal.valueOf(e.getTotalCalendarDays()), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal totalDeductions = nz(e.getTotalDeductions());
        BigDecimal monthlyWages = gross.subtract(totalDeductions);

        return new BigDecimal[] {
                basic, da, basicPlusDa,
                BigDecimal.valueOf(e.getTotalCalendarDays()), BigDecimal.valueOf(e.getTotalCalendarDays()), perDay,
                BigDecimal.valueOf(e.getPresentDays()), nz(e.getPaidLeaveDays()),
                basicPlusDa, hra, gross,
                nz(e.getEpfEmployee()), nz(e.getEsiEmployee()), nz(e.getEpfEmployer()), nz(e.getEsiEmployer()),
                nz(e.getTotalSalaryCtc()), nz(e.getProfessionalTax()), nz(e.getOtherManualDeduction()), nz(e.getAdvanceRecovery()),
                totalDeductions, monthlyWages, nz(e.getAllowance()), nz(e.getNetPay())
        };
    }

    private void writeRow(Row row, int sr, String code, String name, BigDecimal[] values, CellStyle numberStyle, CellStyle highlightStyle) {
        row.createCell(0).setCellValue(sr);
        row.createCell(1).setCellValue(code);
        row.createCell(2).setCellValue(name);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(3 + i);
            cell.setCellValue(values[i].doubleValue());
            cell.setCellStyle(i == values.length - 1 ? highlightStyle : numberStyle); // Net Payment highlighted, like the reference sheet
        }
    }

    private void writeTotalRow(Row row, BigDecimal[] totals, CellStyle numberStyle, CellStyle highlightStyle) {
        row.createCell(2).setCellValue("TOTAL");
        for (int i = 0; i < totals.length; i++) {
            Cell cell = row.createCell(3 + i);
            cell.setCellValue(totals[i].doubleValue());
            cell.setCellStyle(i == totals.length - 1 ? highlightStyle : numberStyle);
        }
    }

    private CellStyle headerStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle numberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
