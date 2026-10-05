package com.example.application.attendance_module.service;

import com.example.application.attendance_module.entity.AttendanceRuleConfig;
import com.example.application.attendance_module.repository.AttendanceRepository;
import com.example.application.client_company_module.repository.ClientCompanyRepository;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.employee_module.service.EmployeeMonthRosterService;
import com.example.application.exit_module.entity.EmployeeExit;
import com.example.application.exit_module.repository.EmployeeExitRepository;
import com.example.application.leave_module.service.EmployeePaidLeaveService;
import com.example.application.leave_module.service.LeavePolicyResolver;
import com.example.application.payroll_module.service.PayrollInputResolver;
import com.example.application.payroll_module.service.PayrollSettingsResolver;
import com.example.application.payroll_module.service.PayrollWorkingDaysResolver;
import com.example.application.site_module.repository.SiteRepository;
import com.example.application.site_module.service.SiteAccessService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Reads the generated Muster Book workbook back and checks WHO is on it - the rule being that an
 * employee appears in a month only if they had joined by that month's end.
 */
@ExtendWith(MockitoExtension.class)
class MonthlyAttendanceReportServiceMusterBookTest {

    private static final Long TENANT = 7L;

    @Mock private EmployeeRepository employeeRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EmployeeSiteAssignmentRepository siteAssignmentRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private PayrollInputResolver payrollInputResolver;
    @Mock private EmployeePaidLeaveService paidLeaveService;
    @Mock private LeavePolicyResolver leavePolicyResolver;
    @Mock private TenantContextService tenantContext;
    @Mock private SiteAccessService siteAccessService;
    @Mock private PayrollSettingsResolver payrollSettingsResolver;
    @Mock private PayrollWorkingDaysResolver payrollWorkingDaysResolver;
    @Mock private ClientCompanyRepository clientCompanyRepository;
    @Mock private AttendanceRuleConfigService attendanceRuleConfigService;
    @Mock private EmployeeExitRepository exitRepository;

    private MonthlyAttendanceReportService service;

    private static Employee employee(long id, String first, LocalDate joined) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName(first);
        e.setLastName("Test");
        e.setEmployeeCode("EMP" + id);
        e.setStatus("ACTIVE");
        e.setJoiningDate(joined);
        e.setClientCompanyId(TENANT);
        return e;
    }

    private final List<Employee> everyone = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // A REAL roster over mocked repositories: this test checks the whole chain (muster -> roster -> rules).
        service = new MonthlyAttendanceReportService(employeeRepository, attendanceRepository, siteAssignmentRepository,
                siteRepository, payrollInputResolver, paidLeaveService, leavePolicyResolver, tenantContext, siteAccessService,
                payrollSettingsResolver, payrollWorkingDaysResolver, clientCompanyRepository, attendanceRuleConfigService,
                new EmployeeMonthRosterService(employeeRepository, exitRepository));

        AttendanceRuleConfig config = new AttendanceRuleConfig();
        config.setWeeklyOffDays("SUNDAY");
        lenient().when(attendanceRuleConfigService.getOrCreateForCurrentTenant()).thenReturn(config);
        lenient().when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT);

        Employee dev = employee(4, "Dev", LocalDate.of(2026, 1, 1));   // left in August
        dev.setStatus("INACTIVE");
        everyone.add(employee(1, "Asha", LocalDate.of(2026, 6, 1)));      // joined in June
        everyone.add(employee(2, "Meera", LocalDate.of(2026, 7, 31)));    // joined on the LAST day of July
        everyone.add(employee(3, "Ravi", LocalDate.of(2026, 8, 10)));     // joined in August
        everyone.add(dev);

        lenient().when(employeeRepository.findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(TENANT, "ACTIVE"))
                .thenReturn(everyone.stream().filter(e -> "ACTIVE".equals(e.getStatus())).toList());
        lenient().when(employeeRepository.findAllById(any())).thenAnswer(inv -> {
            java.util.Collection<Long> ids = inv.getArgument(0);
            return everyone.stream().filter(e -> ids.contains(e.getId())).toList();
        });
        EmployeeExit devExit = new EmployeeExit();
        devExit.setEmployeeId(4L);
        devExit.setStatus("SETTLED");
        devExit.setLastWorkingDay(LocalDate.of(2026, 8, 10));
        lenient().when(exitRepository.findAllByClientCompanyIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of(devExit));
    }

    /** Names on the sheet: rows 2..(last-1) - row 0 is the title, row 1 the headers, the last row the TOTAL line. */
    private List<String> namesOnTheMusterFor(int year, int month) throws Exception {
        byte[] bytes = service.generateMusterBook(year, month, null);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> names = new ArrayList<>();
            for (int r = 2; r < sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                names.add(row.getCell(1).getStringCellValue());
            }
            return names;
        }
    }

    @Test
    void anEmployeeWhoJoinsInAugustIsNotOnJulysMuster() throws Exception {
        assertEquals(false, namesOnTheMusterFor(2026, 7).contains("Ravi Test"));
    }

    @Test
    void anEmployeeWhoJoinedOnTheLastDayOfAMonthIsOnThatMonthsMuster() throws Exception {
        assertEquals(true, namesOnTheMusterFor(2026, 7).contains("Meera Test"));
    }

    @Test
    void anEmployeeWhoLeavesInAugustIsOnJulysMusterBecauseTheyWorkedJuly() throws Exception {
        assertEquals(true, namesOnTheMusterFor(2026, 7).contains("Dev Test"));
    }

    @Test
    void anEmployeeWhoLeavesInAugustIsNotOnAugustsOrLaterMusters() throws Exception {
        assertEquals(false, namesOnTheMusterFor(2026, 8).contains("Dev Test"));
        assertEquals(false, namesOnTheMusterFor(2026, 9).contains("Dev Test"));
    }

    @Test
    void eachMonthsMusterIsExactlyWhoWasWorkingThen() throws Exception {
        assertEquals(List.of("Asha Test", "Dev Test"), namesOnTheMusterFor(2026, 6));
        assertEquals(List.of("Asha Test", "Meera Test", "Dev Test"), namesOnTheMusterFor(2026, 7));
        assertEquals(List.of("Asha Test", "Meera Test", "Ravi Test"), namesOnTheMusterFor(2026, 8));
        assertEquals(List.of("Asha Test", "Meera Test", "Ravi Test"), namesOnTheMusterFor(2026, 9));
    }
}
