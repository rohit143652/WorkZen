package com.example.application.attendance_module.service;

import com.example.application.attendance_module.entity.AttendanceRuleConfig;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stateless working-hours/late/early-exit/status calculator. Deliberately the ONLY place any of
 * this math happens - called from AttendanceService.checkOut() and from
 * AttendanceCorrectionService.approve(), so a normal check-out and an approved correction always
 * produce results computed exactly the same way. Never depend on the frontend to compute any of
 * these numbers; the frontend only ever displays what this class already calculated server-side.
 */
@Component
public class AttendanceRulesEngine {

    public static class WorkResult {
        public int grossWorkMinutes;
        public int breakMinutes;
        public int netWorkMinutes;
        public boolean late;
        public Integer lateMinutes;
        public boolean earlyExit;
        public Integer earlyExitMinutes;
        public String status;
    }

    /** Is the given calendar date this tenant's configured weekly off? */
    public boolean isWeeklyOff(AttendanceRuleConfig config, LocalDate date) {
        Set<DayOfWeek> offDays = parseWeeklyOffDays(config.getWeeklyOffDays());
        return offDays.contains(date.getDayOfWeek());
    }

    private Set<DayOfWeek> parseWeeklyOffDays(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> DayOfWeek.valueOf(s.toUpperCase()))
                .collect(Collectors.toSet());
    }

    /**
     * Computes gross/break/net working minutes, late-coming, early-exit, and the resulting
     * attendance status, given a check-in/check-out pair and the tenant's rules.
     *
     * Example (matching the spec exactly): check-in 09:30, check-out 18:30, office hours
     * 09:30-18:30, grace 10 min, default break 45 min -> gross 540m (9h), break 45m, net 495m
     * (8h15m); on-time (checked in AT start, not after grace); on-time exit (checked out AT end).
     */
    public WorkResult compute(LocalDateTime checkIn, LocalDateTime checkOut, AttendanceRuleConfig config) {
        WorkResult result = new WorkResult();

        long grossMinutes = Math.max(0, Duration.between(checkIn, checkOut).toMinutes());
        int breakMinutes = config.getDefaultBreakMinutes() == null ? 0 : config.getDefaultBreakMinutes();
        // Never subtract more break time than was actually worked - avoids a negative net for a
        // very short day (e.g. checking out 20 minutes after checking in).
        breakMinutes = (int) Math.min(breakMinutes, grossMinutes);
        long netMinutes = grossMinutes - breakMinutes;

        result.grossWorkMinutes = (int) grossMinutes;
        result.breakMinutes = breakMinutes;
        result.netWorkMinutes = (int) netMinutes;

        LocalDateTime officeStart = checkIn.toLocalDate().atTime(config.getOfficeStartTime());
        LocalDateTime graceCutoff = officeStart.plusMinutes(config.getLateGraceMinutes() == null ? 0 : config.getLateGraceMinutes());
        if (checkIn.isAfter(graceCutoff)) {
            result.late = true;
            // Measured from the grace cutoff, not from office start - matches the spec's own
            // worked example exactly (start 09:30, grace 10min -> cutoff 09:40, check-in 09:50
            // -> "Late By 10 minutes", not 20).
            result.lateMinutes = (int) Duration.between(graceCutoff, checkIn).toMinutes();
        } else {
            result.late = false;
            result.lateMinutes = null;
        }

        LocalDateTime officeEnd = checkOut.toLocalDate().atTime(config.getOfficeEndTime());
        if (checkOut.isBefore(officeEnd)) {
            result.earlyExit = true;
            result.earlyExitMinutes = (int) Duration.between(checkOut, officeEnd).toMinutes();
        } else {
            result.earlyExit = false;
            result.earlyExitMinutes = null;
        }

        Integer fullDayMin = config.getFullDayMinMinutes();
        Integer halfDayMin = config.getHalfDayMinMinutes();
        if (fullDayMin != null && netMinutes >= fullDayMin) {
            result.status = "PRESENT";
        } else if (halfDayMin != null && netMinutes >= halfDayMin) {
            result.status = "HALF_DAY";
        } else {
            result.status = "ABSENT";
        }

        return result;
    }
}
