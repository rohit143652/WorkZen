package com.example.application.employee_module.service;

import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.exit_module.entity.EmployeeExit;
import com.example.application.exit_module.repository.EmployeeExitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WHO belongs in a given month's reports and payroll - the single definition the monthly attendance
 * report, the Muster Book and the payroll run all use, so they can never disagree with each other.
 *
 * An employee belongs to month M when they were employed during the months up to - but not
 * including - the month they left:
 *   - JOINED: they had joined by the last day of M. Joining on any day of M (even the 31st) puts
 *     them in M; joining the day after puts them in the NEXT month, not M.
 *   - LEFT: the month their last working day falls in is NOT theirs. Full &amp; Final Settlement
 *     already pays that month's (prorated) salary, so listing them in that month's payroll would pay
 *     the same month twice. Every month BEFORE it still shows them, because they worked it.
 *
 * Who counts as "left", precisely:
 *   - an ACTIVE employee is only affected by a PENDING exit (INITIATED, i.e. serving notice). A
 *     SETTLED exit on an ACTIVE employee is history - they were re-hired or re-activated since.
 *   - an employee who is no longer ACTIVE is placed by their latest exit's last working day.
 *   - an employee deactivated WITHOUT an exit record has no known leaving date, so (exactly as
 *     before) they are not guessed into any month.
 */
@Service
public class EmployeeMonthRosterService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeExitRepository exitRepository;

    public EmployeeMonthRosterService(EmployeeRepository employeeRepository, EmployeeExitRepository exitRepository) {
        this.employeeRepository = employeeRepository;
        this.exitRepository = exitRepository;
    }

    /** Everyone who belongs in {@code month}'s reports/payroll, ordered by employee code. */
    @Transactional(readOnly = true)
    public List<Employee> employeesForMonth(Long tenantId, YearMonth month) {
        LocalDate monthEnd = month.atEndOfMonth();
        List<EmployeeExit> exits = exitRepository.findAllByClientCompanyIdOrderByCreatedAtDesc(tenantId);
        Map<Long, LocalDate> pendingExitEnd = lastWorkingDayByEmployee(exits, true);
        Map<Long, LocalDate> anyExitEnd = lastWorkingDayByEmployee(exits, false);

        Map<Long, Employee> roster = new LinkedHashMap<>();
        Set<Long> activeIds = new HashSet<>();
        for (Employee e : employeeRepository.findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(tenantId, "ACTIVE")) {
            activeIds.add(e.getId());
            if (e.hasJoinedBy(monthEnd) && !hasLeftByEndOf(pendingExitEnd.get(e.getId()), monthEnd)) {
                roster.put(e.getId(), e);
            }
        }

        // People who have since LEFT are no longer ACTIVE, so the query above never sees them - but they
        // still belong to every month before the month they left.
        Set<Long> leaverIds = anyExitEnd.entrySet().stream()
                .filter(entry -> !activeIds.contains(entry.getKey()) && entry.getValue().isAfter(monthEnd))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!leaverIds.isEmpty()) {
            for (Employee e : employeeRepository.findAllById(leaverIds)) {
                if (tenantId.equals(e.getClientCompanyId()) && !"ACTIVE".equals(e.getStatus()) && e.hasJoinedBy(monthEnd)) {
                    roster.put(e.getId(), e);
                }
            }
        }

        return roster.values().stream()
                .sorted(Comparator.comparing(Employee::getEmployeeCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    /**
     * Of these employees, the ones who do NOT belong in {@code month} because they had not joined by
     * its end, or had left by it. Used to clear a stale payroll row left by an earlier calculation.
     * Deliberately narrower than "not in employeesForMonth": someone deactivated without an exit
     * record is not flagged, so their existing row is never removed on a guess.
     */
    @Transactional(readOnly = true)
    public Set<Long> employeeIdsOutsideEmploymentWindow(Long tenantId, YearMonth month, Collection<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Set.of();
        }
        LocalDate monthEnd = month.atEndOfMonth();
        List<EmployeeExit> exits = exitRepository.findAllByClientCompanyIdOrderByCreatedAtDesc(tenantId);
        Map<Long, LocalDate> pendingExitEnd = lastWorkingDayByEmployee(exits, true);
        Map<Long, LocalDate> anyExitEnd = lastWorkingDayByEmployee(exits, false);

        Set<Long> outside = new HashSet<>();
        for (Employee e : employeeRepository.findAllById(employeeIds)) {
            LocalDate end = "ACTIVE".equals(e.getStatus()) ? pendingExitEnd.get(e.getId()) : anyExitEnd.get(e.getId());
            if (!e.hasJoinedBy(monthEnd) || hasLeftByEndOf(end, monthEnd)) {
                outside.add(e.getId());
            }
        }
        return outside;
    }

    /** The month of the last working day is not theirs, so "left by the end of M" means a last working day on or before M's last day. */
    private static boolean hasLeftByEndOf(LocalDate lastWorkingDay, LocalDate monthEnd) {
        return lastWorkingDay != null && !lastWorkingDay.isAfter(monthEnd);
    }

    /** Latest last working day per employee, from pending exits only or from every exit. */
    private static Map<Long, LocalDate> lastWorkingDayByEmployee(List<EmployeeExit> exits, boolean pendingOnly) {
        return exits.stream()
                .filter(x -> x.getEmployeeId() != null && x.getLastWorkingDay() != null)
                .filter(x -> !pendingOnly || "INITIATED".equals(x.getStatus()))
                .collect(Collectors.toMap(EmployeeExit::getEmployeeId, EmployeeExit::getLastWorkingDay,
                        (a, b) -> a.isAfter(b) ? a : b));
    }
}
