package com.example.application.employee_module.service;

import com.example.application.common.exception.BadRequestException;
import com.example.application.department_module.dto.DepartmentRequest;
import com.example.application.department_module.dto.DepartmentResponse;
import com.example.application.department_module.service.DepartmentService;
import com.example.application.designation_module.dto.DesignationRequest;
import com.example.application.designation_module.dto.DesignationResponse;
import com.example.application.designation_module.service.DesignationService;
import com.example.application.employee_module.dto.EmployeeBulkImportResult;
import com.example.application.employee_module.dto.EmployeeRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bulk Employee Import (Excel upload) - reads a .xlsx file of employee rows and creates each one
 * through the SAME EmployeeService.create() every single-employee "Add" already goes through, so
 * every validation rule (required fields, duplicate email, etc.) behaves identically here with
 * zero duplicated logic.
 *
 * Department and Designation get special handling, since free-text spreadsheet cells are prone
 * to typos in a way a dropdown-based single "Add Employee" form never is - see
 * resolveDepartment()/resolveDesignation():
 *   - An exact (case/whitespace-insensitive) match to an existing one is used as-is.
 *   - A close-but-not-exact spelling (>= 90% similarity, Levenshtein-based) is treated as that
 *     SAME existing department/designation, rather than creating a near-duplicate.
 *   - Only a genuinely new name (below 90% similarity to anything on file) gets auto-created.
 * Matches are resolved against a list that grows AS THE IMPORT RUNS - if row 5 creates
 * "Marketing" and row 40 has "Markting", row 40 matches row 5's new department instead of
 * creating a second near-duplicate, even though "Marketing" didn't exist before this import
 * started.
 *
 * Deliberately "best effort", not all-or-nothing: each row is its own independent attempt (own
 * try/catch, own transaction via EmployeeService.create()) - a typo elsewhere in row 47 doesn't
 * undo the 46 good rows before it.
 *
 * Only the core "add an employee" fields are supported here (name, contact, dates, department/
 * designation, address) - salary structure assignment and login setup are deliberately left out
 * of bulk import; those are done per-employee afterward, same as when adding one at a time
 * without immediately setting up salary/login either.
 */
@Service
public class EmployeeBulkImportService {

    private static final String[] TEMPLATE_HEADERS = {
            "First Name*", "Middle Name", "Last Name*", "Email*", "Mobile Number",
            "Date of Birth (YYYY-MM-DD)", "Gender", "Joining Date (YYYY-MM-DD)*",
            "Department*", "Designation*", "Employment Type", "Address", "City", "State", "Country", "Pincode"
    };
    private static final int DEPARTMENT_COL = 8;
    private static final int DESIGNATION_COL = 9;
    private static final double FUZZY_MATCH_THRESHOLD = 90.0;

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final DesignationService designationService;

    public EmployeeBulkImportService(EmployeeService employeeService, DepartmentService departmentService,
                                      DesignationService designationService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.designationService = designationService;
    }

    public EmployeeBulkImportResult importFromExcel(MultipartFile file, Long actorId, HttpServletRequest httpRequest) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded.");
        }

        // Grows during the loop as new departments/designations get auto-created, so later rows
        // in the SAME file match against ones created earlier in this same run - see class javadoc.
        Map<String, String> knownDepartments = loadExistingNames(departmentService.findAll(false).stream().map(DepartmentResponse::getName).toList());
        Map<String, String> knownDesignations = loadExistingNames(designationService.findAll(false).stream().map(DesignationResponse::getName).toList());

        List<EmployeeBulkImportResult.RowError> errors = new ArrayList<>();
        int totalRows = 0;
        int successCount = 0;

        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            // Row 0 is the header row - data starts at row 1 (Excel row 2, since spreadsheets are 1-indexed for humans).
            for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowBlank(row)) continue;

                int excelRowNumber = rowIndex + 1;
                totalRows++;
                try {
                    EmployeeRequest request = parseRow(row);
                    request.setDepartment(resolveName(request.getDepartment(), knownDepartments,
                            name -> createDepartment(name, actorId, httpRequest)));
                    request.setDesignation(resolveName(request.getDesignation(), knownDesignations,
                            name -> createDesignation(name, actorId, httpRequest)));

                    employeeService.create(request, actorId, httpRequest);
                    successCount++;
                } catch (Exception rowFailure) {
                    String reason = rowFailure.getMessage() != null ? rowFailure.getMessage() : rowFailure.getClass().getSimpleName();
                    errors.add(new EmployeeBulkImportResult.RowError(excelRowNumber, reason));
                }
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof BadRequestException) throw (BadRequestException) e;
            throw new BadRequestException("Unable to read this file - make sure it's a valid .xlsx file matching the template.");
        }

        return new EmployeeBulkImportResult(totalRows, successCount, errors.size(), errors);
    }

    /** A downloadable starting point with the exact expected headers, one example row, and dropdown lists for Department/Designation populated from what already exists. */
    public byte[] generateTemplate() {
        List<String> departmentNames = departmentService.findAll(false).stream().map(DepartmentResponse::getName).toList();
        List<String> designationNames = designationService.findAll(false).stream().map(DesignationResponse::getName).toList();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Employees");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            Row exampleRow = sheet.createRow(1);
            String[] example = {
                    "Rohit", "", "Patil", "rohit.patil.example@company.com", "9876543210",
                    "1995-06-15", "Male", "2026-01-15", "Operations", "Site Supervisor",
                    "FULL_TIME", "123 MG Road", "Pune", "Maharashtra", "India", "411001"
            };
            for (int i = 0; i < example.length; i++) {
                exampleRow.createCell(i).setCellValue(example[i]);
            }

            // The instructional note is on its OWN sheet, deliberately NOT in the same
            // "Employees" data sheet as actual rows - it used to sit in row 4 of that same
            // sheet, and the import logic (which reads every non-blank row as a candidate
            // employee) tried to parse that entire sentence as a "First Name" value, failing
            // with a MySQL "data too long" error. A separate tab can never be mistaken for data.
            Sheet instructionsSheet = workbook.createSheet("Instructions");
            instructionsSheet.setColumnWidth(0, 120 * 256);
            instructionsSheet.createRow(0).createCell(0).setCellValue(
                    "Fields marked * are required. Department and Designation have dropdowns on the Employees tab with your current list - "
                            + "picking from them avoids typos, but a new name typed in is fine too (a close spelling reuses the existing one; a genuinely new name is added automatically). "
                            + "Delete the example row (row 2 of the Employees tab) before uploading your real data.");

            // Hidden helper sheet backing the dropdown lists - Excel data validation needs a real
            // cell range to point at (not just an inline list) once you have more than a handful
            // of values, so the actual Department/Designation names live here instead.
            XSSFSheet listSheet = workbook.createSheet("Lists");
            for (int i = 0; i < departmentNames.size(); i++) {
                listSheet.createRow(i).createCell(0).setCellValue(departmentNames.get(i));
            }
            for (int i = 0; i < designationNames.size(); i++) {
                Row r = listSheet.getRow(i);
                if (r == null) r = listSheet.createRow(i);
                r.createCell(1).setCellValue(designationNames.get(i));
            }
            workbook.setSheetHidden(workbook.getSheetIndex(listSheet), true);

            int maxDataRow = 500; // generous headroom for a bulk import file
            addDropdownValidation(sheet, listSheet, DEPARTMENT_COL, departmentNames.size(), "A", maxDataRow);
            addDropdownValidation(sheet, listSheet, DESIGNATION_COL, designationNames.size(), "B", maxDataRow);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate the import template", e);
        }
    }

    private void addDropdownValidation(XSSFSheet dataSheet, XSSFSheet listSheet, int column, int listSize, String listColumnLetter, int maxDataRow) {
        if (listSize == 0) return; // nothing to list yet - a brand new tenant with no departments/designations set up
        String formula = "Lists!$" + listColumnLetter + "$1:$" + listColumnLetter + "$" + listSize;
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(dataSheet);
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        CellRangeAddressList addressList = new CellRangeAddressList(1, maxDataRow, column, column);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(false); // a name not yet on the list is still accepted - see resolveName()
        dataSheet.addValidationData(validation);
    }

    // ---- Department/Designation fuzzy resolution ----

    private interface NameCreator {
        String create(String name);
    }

    private String resolveName(String rawName, Map<String, String> knownNames, NameCreator creator) {
        if (rawName == null || rawName.isBlank()) return rawName;
        String trimmed = rawName.trim();
        String normalized = normalize(trimmed);

        String exact = knownNames.get(normalized);
        if (exact != null) return exact;

        String bestMatch = null;
        double bestScore = 0;
        for (Map.Entry<String, String> entry : knownNames.entrySet()) {
            double score = similarityPercent(normalized, entry.getKey());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry.getValue();
            }
        }
        if (bestMatch != null && bestScore >= FUZZY_MATCH_THRESHOLD) {
            return bestMatch;
        }

        String created = creator.create(trimmed);
        knownNames.put(normalize(created), created);
        return created;
    }

    private String createDepartment(String name, Long actorId, HttpServletRequest httpRequest) {
        DepartmentRequest request = new DepartmentRequest();
        request.setName(name);
        return departmentService.create(request, actorId, httpRequest).getName();
    }

    private String createDesignation(String name, Long actorId, HttpServletRequest httpRequest) {
        DesignationRequest request = new DesignationRequest();
        request.setName(name);
        return designationService.create(request, actorId, httpRequest).getName();
    }

    private Map<String, String> loadExistingNames(List<String> names) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : names) {
            map.put(normalize(name), name);
        }
        return map;
    }

    private String normalize(String s) {
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /** Levenshtein-distance-based similarity, 0-100. */
    private double similarityPercent(String a, String b) {
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 100.0;
        int distance = levenshteinDistance(a, b);
        return (1.0 - (double) distance / maxLen) * 100.0;
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    // ---- Row parsing ----

    private boolean isRowBlank(Row row) {
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            if (!getCellString(row, i).isBlank()) return false;
        }
        return true;
    }

    private EmployeeRequest parseRow(Row row) {
        EmployeeRequest request = new EmployeeRequest();
        request.setFirstName(getCellString(row, 0));
        request.setMiddleName(blankToNull(getCellString(row, 1)));
        request.setLastName(getCellString(row, 2));
        request.setEmail(getCellString(row, 3));
        request.setMobileNumber(blankToNull(getCellString(row, 4)));
        request.setDateOfBirth(getCellDate(row, 5));
        request.setGender(blankToNull(getCellString(row, 6)));
        request.setJoiningDate(getCellDate(row, 7));
        request.setDepartment(getCellString(row, DEPARTMENT_COL));
        request.setDesignation(getCellString(row, DESIGNATION_COL));
        request.setEmploymentType(blankToNull(getCellString(row, 10)));
        request.setAddress(blankToNull(getCellString(row, 11)));
        request.setCity(blankToNull(getCellString(row, 12)));
        request.setState(blankToNull(getCellString(row, 13)));
        request.setCountry(blankToNull(getCellString(row, 14)));
        request.setPincode(blankToNull(getCellString(row, 15)));
        // Employee code, salary structure, and login are deliberately left unset here - see
        // class javadoc. EmployeeService.create() auto-generates the code when left blank,
        // exactly as it does for a single manually-added employee.

        // A clear, actionable error instead of a raw MySQL "data too long" exception if a cell
        // holds far more text than any real name/field ever would (e.g. a stray note or comment
        // accidentally left in the data area rather than its own sheet).
        requireReasonableLength(request.getFirstName(), "First Name", 100);
        requireReasonableLength(request.getLastName(), "Last Name", 100);
        requireReasonableLength(request.getEmail(), "Email", 150);

        return request;
    }

    private void requireReasonableLength(String value, String fieldLabel, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new BadRequestException(fieldLabel + " is " + value.length() + " characters - far longer than a real value should be "
                    + "(max " + maxLength + "). Check this row doesn't contain a note or comment instead of actual data.");
        }
    }

    private String blankToNull(String s) {
        return s.isBlank() ? null : s;
    }

    private String getCellString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                double value = cell.getNumericCellValue();
                yield (value == Math.floor(value)) ? String.valueOf((long) value) : String.valueOf(value);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> "";
            default -> cell.toString().trim();
        };
    }

    private LocalDate getCellDate(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = getCellString(row, index);
        if (text.isBlank()) return null;
        try {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            throw new BadRequestException("Invalid date \"" + text + "\" - use YYYY-MM-DD format.");
        }
    }
}
