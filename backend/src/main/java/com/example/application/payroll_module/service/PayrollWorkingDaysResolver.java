package com.example.application.payroll_module.service;

import com.example.application.attendance_module.service.AttendanceRuleConfigService;
import com.example.application.payroll_module.entity.PayrollSettings;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves PayrollSettings.workingDaysBasis to an actual day count for one payroll month -
 * previously this was hardcoded to yearMonth.lengthOfMonth() (always calendar days) everywhere
 * daysInMonth was needed, with no company-level choice at all (spec section 3).
 *
 * This is the denominator used throughout payroll proration (PayrollInputResolver: daily rate =
 * Basic+DA / this figure). Three bases, resolved for the CURRENT tenant's configuration:
 *   - CALENDAR_DAYS: yearMonth.lengthOfMonth() - the prior, only behavior, still the default.
 *   - WORKING_DAYS: calendar days minus that month's configured weekly-off days (reuses the
 *     existing AttendanceRuleConfig.weeklyOffDays - no separate payroll-specific weekly-off
 *     configuration is introduced, since one already exists and nothing suggests payroll should
 *     disagree with attendance about which days of the week are offs).
 *   - FIXED: PayrollSettings.fixedWorkingDays regardless of the actual month (common conventions
 *     are 26 or 30, but any company-chosen value is accepted - falls back to calendar days if
 *     the company selected FIXED but never actually set a value, rather than dividing by zero
 *     or an undefined denominator).
 */
@Service
public class PayrollWorkingDaysResolver {

    private final AttendanceRuleConfigService attendanceRuleConfigService;

    public PayrollWorkingDaysResolver(AttendanceRuleConfigService attendanceRuleConfigService) {
        this.attendanceRuleConfigService = attendanceRuleConfigService;
    }

    public int resolve(PayrollSettings settings, YearMonth yearMonth) {
        String basis = settings.getWorkingDaysBasis() != null ? settings.getWorkingDaysBasis() : "CALENDAR_DAYS";
        return switch (basis) {
            case "WORKING_DAYS" -> resolveWorkingDays(yearMonth);
            case "FIXED" -> settings.getFixedWorkingDays() != null && settings.getFixedWorkingDays() > 0
                    ? settings.getFixedWorkingDays()
                    : yearMonth.lengthOfMonth();
            default -> yearMonth.lengthOfMonth(); // CALENDAR_DAYS
        };
    }

    private int resolveWorkingDays(YearMonth yearMonth) {
        String weeklyOffDaysCsv = attendanceRuleConfigService.getOrCreateForCurrentTenant().getWeeklyOffDays();
        Set<DayOfWeek> weeklyOffs = (weeklyOffDaysCsv == null || weeklyOffDaysCsv.isBlank())
                ? Set.of()
                : Arrays.stream(weeklyOffDaysCsv.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .map(DayOfWeek::valueOf)
                        .collect(Collectors.toSet());

        int calendarDays = yearMonth.lengthOfMonth();
        if (weeklyOffs.isEmpty()) return calendarDays; // No weekly offs configured - working days == calendar days.

        int offDaysInMonth = 0;
        for (int day = 1; day <= calendarDays; day++) {
            if (weeklyOffs.contains(LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), day).getDayOfWeek())) {
                offDaysInMonth++;
            }
        }
        int workingDays = calendarDays - offDaysInMonth;
        return workingDays > 0 ? workingDays : calendarDays; // Never divide by zero/negative - falls back safely.
    }
}
