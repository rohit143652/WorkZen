package com.example.application.employee_module.service;

import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.exit_module.entity.EmployeeExit;
import com.example.application.exit_module.repository.EmployeeExitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * WHO belongs in a month's reports and payroll. One shared rule, so the muster, the summary report and
 * payroll can never disagree: joined by the month's end, and not already gone by it - where the month
 * someone LEAVES belongs to Full &amp; Final Settlement, not to payroll (it already pays that month).
 */
@ExtendWith(MockitoExtension.class)
class EmployeeMonthRosterServiceTest {

    private static final Long TENANT = 7L;
    private static final YearMonth JUNE = YearMonth.of(2026, 6);
    private static final YearMonth JULY = YearMonth.of(2026, 7);
    private static final YearMonth AUGUST = YearMonth.of(2026, 8);
    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeExitRepository exitRepository;

    private EmployeeMonthRosterService service;

    private Employee stays;          // ACTIVE since January, never leaves
    private Employee joinsInAugust;  // ACTIVE, joined 10 Aug
    private Employee leftInAugust;   // INACTIVE, settled exit with last working day 10 Aug
    private Employee onNotice;       // ACTIVE, serving notice - last working day 20 Aug (exit INITIATED)
    private Employee deactivated;    // INACTIVE, deactivated by hand: NO exit record, so no known leaving date
    private Employee rehired;        // ACTIVE again - an OLD settled exit (March) is history
    private Employee leftOnJulyEnd;  // INACTIVE, last working day = 31 Jul (the last day of July)

    private final List<Employee> everyone = new ArrayList<>();

    private Employee employee(long id, String status, LocalDate joined) {
        Employee e = new Employee();
        e.setId(id);
        e.setEmployeeCode(String.format("EMP%04d", id));
        e.setFirstName("Emp" + id);
        e.setLastName("Test");
        e.setStatus(status);
        e.setJoiningDate(joined);
        e.setClientCompanyId(TENANT);
        everyone.add(e);
        return e;
    }

    private EmployeeExit exit(Employee e, String status, LocalDate lastWorkingDay) {
        EmployeeExit x = new EmployeeExit();
        x.setEmployeeId(e.getId());
        x.setStatus(status);
        x.setLastWorkingDay(lastWorkingDay);
        return x;
    }

    @BeforeEach
    void setUp() {
        service = new EmployeeMonthRosterService(employeeRepository, exitRepository);

        stays = employee(1, "ACTIVE", LocalDate.of(2026, 1, 1));
        joinsInAugust = employee(2, "ACTIVE", LocalDate.of(2026, 8, 10));
        leftInAugust = employee(3, "INACTIVE", LocalDate.of(2026, 1, 1));
        onNotice = employee(4, "ACTIVE", LocalDate.of(2026, 1, 1));
        deactivated = employee(5, "INACTIVE", LocalDate.of(2026, 1, 1));
        rehired = employee(6, "ACTIVE", LocalDate.of(2026, 1, 1));
        leftOnJulyEnd = employee(7, "INACTIVE", LocalDate.of(2026, 1, 1));

        lenient().when(employeeRepository.findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(TENANT, "ACTIVE"))
                .thenReturn(everyone.stream().filter(e -> "ACTIVE".equals(e.getStatus())).toList());
        lenient().when(employeeRepository.findAllById(any())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return everyone.stream().filter(e -> ids.contains(e.getId())).toList();
        });
        lenient().when(exitRepository.findAllByClientCompanyIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of(
                exit(leftInAugust, "SETTLED", LocalDate.of(2026, 8, 10)),
                exit(onNotice, "INITIATED", LocalDate.of(2026, 8, 20)),
                exit(rehired, "SETTLED", LocalDate.of(2026, 3, 5)),
                exit(leftOnJulyEnd, "SETTLED", LocalDate.of(2026, 7, 31))));
    }

    private List<Long> idsFor(YearMonth month) {
        return service.employeesForMonth(TENANT, month).stream().map(Employee::getId).toList();
    }

    // ===================== joining =====================

    @Test
    void someoneWhoJoinsInAugustIsNotInJuly() {
        assertFalse(idsFor(JULY).contains(2L));
    }

    @Test
    void someoneWhoJoinsInAugustIsInAugustAndAfter() {
        assertTrue(idsFor(AUGUST).contains(2L));
        assertTrue(idsFor(SEPTEMBER).contains(2L));
    }

    // ===================== leaving =====================

    @Test
    void someoneWhoLeftInAugustStillAppearsInTheMonthsTheyWorked() {
        assertTrue(idsFor(JULY).contains(3L), "worked all of July -> in July's report");
        assertTrue(idsFor(JUNE).contains(3L));
    }

    @Test
    void theMonthSomeoneLeavesIsNotTheirsBecauseFullAndFinalSettlementPaysIt() {
        assertFalse(idsFor(AUGUST).contains(3L), "left on 10 Aug -> not in August payroll/report");
    }

    @Test
    void someoneWhoLeftIsNotInAnyLaterMonth() {
        assertFalse(idsFor(SEPTEMBER).contains(3L));
    }

    @Test
    void aLastWorkingDayOnTheLastDayOfAMonthStillMakesThatMonthTheirExitMonth() {
        assertFalse(idsFor(JULY).contains(7L), "last working day 31 Jul -> July is their exit month");
        assertTrue(idsFor(JUNE).contains(7L));
    }

    @Test
    void someoneServingNoticeIsInReportsUntilTheMonthTheirNoticeEnds() {
        assertTrue(idsFor(JULY).contains(4L));
        assertFalse(idsFor(AUGUST).contains(4L), "still ACTIVE, but their last working day is in August");
        assertFalse(idsFor(SEPTEMBER).contains(4L));
    }

    @Test
    void anOldSettledExitOnAnActiveEmployeeIsHistoryNotAReasonToHideThem() {
        // Re-hired / re-activated since - they are ACTIVE again, so their March exit must not hide them.
        assertTrue(idsFor(JULY).contains(6L));
        assertTrue(idsFor(AUGUST).contains(6L));
    }

    @Test
    void someoneDeactivatedWithoutAnExitRecordIsNotGuessedIntoAnyMonth() {
        assertFalse(idsFor(JUNE).contains(5L));
        assertFalse(idsFor(JULY).contains(5L));
    }

    @Test
    void aLeaverOfAnotherCompanyIsNeverIncluded() {
        leftInAugust.setClientCompanyId(99L);
        assertFalse(idsFor(JULY).contains(3L));
    }

    // ===================== whole months =====================

    @Test
    void theWholeRosterForEachMonthIsExactlyWhoWasWorkingThen() {
        assertEquals(List.of(1L, 3L, 4L, 6L, 7L), idsFor(JUNE));              // everyone employed in June
        assertEquals(List.of(1L, 3L, 4L, 6L), idsFor(JULY));                  // 2 not yet joined, 7 left in July
        assertEquals(List.of(1L, 2L, 6L), idsFor(AUGUST));                    // 2 joined; 3 and 4 leave in August
        assertEquals(List.of(1L, 2L, 6L), idsFor(SEPTEMBER));
    }

    @Test
    void theRosterIsOrderedByEmployeeCodeEvenWhenLeaversAreMergedIn() {
        List<String> codes = service.employeesForMonth(TENANT, JULY).stream().map(Employee::getEmployeeCode).toList();
        assertEquals(codes.stream().sorted().toList(), codes);
    }

    // ===================== clearing stale payroll rows =====================

    @Test
    void staleRowsAreFlaggedOnlyForThoseWhoHadNotJoinedOrHadLeftByThatMonth() {
        Set<Long> outsideJuly = service.employeeIdsOutsideEmploymentWindow(TENANT, JULY, List.of(1L, 2L, 3L, 4L, 5L, 7L));

        assertEquals(Set.of(2L, 7L), outsideJuly, "2 joins in August; 7's exit month is July");
    }

    @Test
    void someoneDeactivatedWithoutAnExitKeepsTheirExistingRow() {
        assertFalse(service.employeeIdsOutsideEmploymentWindow(TENANT, JULY, List.of(5L)).contains(5L));
    }

    @Test
    void anActiveEmployeeWithOnlyAnOldSettledExitIsNeverFlagged() {
        assertTrue(service.employeeIdsOutsideEmploymentWindow(TENANT, AUGUST, List.of(6L)).isEmpty());
    }

    @Test
    void noRowsMeansNothingToFlag() {
        assertTrue(service.employeeIdsOutsideEmploymentWindow(TENANT, JULY, List.of()).isEmpty());
    }
}
