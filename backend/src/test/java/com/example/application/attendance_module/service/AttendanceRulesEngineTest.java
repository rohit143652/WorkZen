package com.example.application.attendance_module.service;

import com.example.application.attendance_module.entity.AttendanceRuleConfig;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AttendanceRulesEngine is pure/stateless (no repository, no tenant context) so these are plain
 * unit tests, no Mockito needed - exactly the point of keeping all the working-hours/late/early/
 * status math in one dependency-free class (see the class javadoc).
 */
class AttendanceRulesEngineTest {

    private final AttendanceRulesEngine engine = new AttendanceRulesEngine();

    private AttendanceRuleConfig defaultConfig() {
        AttendanceRuleConfig config = new AttendanceRuleConfig();
        config.setOfficeStartTime(LocalTime.of(9, 30));
        config.setOfficeEndTime(LocalTime.of(18, 30));
        config.setRequiredWorkingMinutes(480);
        config.setHalfDayMinMinutes(240);
        config.setFullDayMinMinutes(420);
        config.setLateGraceMinutes(10);
        config.setDefaultBreakMinutes(45);
        config.setAllowMultipleCheckin(false);
        config.setWeeklyOffDays("SUNDAY");
        return config;
    }

    // ---- The exact example from the spec: check-in 09:30, check-out 18:30 ----
    @Test
    void computesGrossBreakNetExactlyLikeTheSpecExample() {
        AttendanceRuleConfig config = defaultConfig();
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 30);
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(18, 30);

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertEquals(540, result.grossWorkMinutes); // 9h
        assertEquals(45, result.breakMinutes);
        assertEquals(495, result.netWorkMinutes); // 8h15m
        assertEquals("PRESENT", result.status); // 495 >= fullDayMinMinutes (420)
    }

    // ---- Late-coming: the exact example from the spec ----
    @Test
    void checkInAfterGraceCutoffIsLate() {
        AttendanceRuleConfig config = defaultConfig();
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 50); // start 09:30 + 10min grace = cutoff 09:40
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(18, 30);

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertTrue(result.late);
        assertEquals(10, result.lateMinutes); // measured from the 09:40 grace cutoff, matching the spec's own example exactly
    }

    @Test
    void checkInWithinGraceIsNotLate() {
        AttendanceRuleConfig config = defaultConfig();
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 38); // within the 10-minute grace window
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(18, 30);

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertFalse(result.late);
        assertNull(result.lateMinutes);
    }

    @Test
    void checkInExactlyAtGraceCutoffIsNotLate() {
        AttendanceRuleConfig config = defaultConfig();
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 40); // exactly at the cutoff (start + grace)
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(18, 30);

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertFalse(result.late);
    }

    // ---- Early exit: the exact example from the spec ----
    @Test
    void checkOutBeforeOfficeEndIsEarlyExit() {
        AttendanceRuleConfig config = defaultConfig();
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 30);
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(17, 45);

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertTrue(result.earlyExit);
        assertEquals(45, result.earlyExitMinutes);
    }

    @Test
    void checkOutAtOrAfterOfficeEndIsNotEarlyExit() {
        AttendanceRuleConfig config = defaultConfig();
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 30);
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(18, 30);

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertFalse(result.earlyExit);
        assertNull(result.earlyExitMinutes);
    }

    // ---- Half-day / full-day / absent thresholds ----
    @Test
    void netMinutesAboveFullDayThresholdIsPresent() {
        AttendanceRuleConfig config = defaultConfig(); // fullDayMin = 420
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 0);
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(17, 0); // 480 gross - 45 break = 435 net

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertEquals("PRESENT", result.status);
    }

    @Test
    void netMinutesBetweenHalfAndFullDayThresholdIsHalfDay() {
        AttendanceRuleConfig config = defaultConfig(); // halfDayMin = 240, fullDayMin = 420
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 0);
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(14, 0); // 300 gross - 45 break = 255 net

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertEquals("HALF_DAY", result.status);
    }

    @Test
    void netMinutesBelowHalfDayThresholdIsAbsent() {
        AttendanceRuleConfig config = defaultConfig(); // halfDayMin = 240
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 0);
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(10, 30); // 90 gross - 45 break = 45 net, well below 240

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertEquals("ABSENT", result.status);
    }

    // ---- Break minutes should never exceed the actual gross duration (a very short day) ----
    @Test
    void breakMinutesNeverExceedsGrossMinutes() {
        AttendanceRuleConfig config = defaultConfig(); // defaultBreakMinutes = 45
        LocalDateTime checkIn = LocalDate.of(2026, 1, 5).atTime(9, 0);
        LocalDateTime checkOut = checkIn.toLocalDate().atTime(9, 20); // only 20 minutes gross

        AttendanceRulesEngine.WorkResult result = engine.compute(checkIn, checkOut, config);

        assertEquals(20, result.grossWorkMinutes);
        assertEquals(20, result.breakMinutes); // capped, not 45
        assertEquals(0, result.netWorkMinutes); // never negative
    }

    // ---- Weekly off ----
    @Test
    void weeklyOffDayIsRecognized() {
        AttendanceRuleConfig config = defaultConfig(); // weeklyOffDays = "SUNDAY"
        LocalDate sunday = LocalDate.of(2026, 1, 4);
        assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());

        assertTrue(engine.isWeeklyOff(config, sunday));
        assertFalse(engine.isWeeklyOff(config, sunday.plusDays(1))); // Monday
    }

    @Test
    void multipleWeeklyOffDaysAreAllRecognized() {
        AttendanceRuleConfig config = defaultConfig();
        config.setWeeklyOffDays("SATURDAY,SUNDAY");

        LocalDate saturday = LocalDate.of(2026, 1, 3);
        LocalDate sunday = LocalDate.of(2026, 1, 4);
        LocalDate monday = LocalDate.of(2026, 1, 5);

        assertTrue(engine.isWeeklyOff(config, saturday));
        assertTrue(engine.isWeeklyOff(config, sunday));
        assertFalse(engine.isWeeklyOff(config, monday));
    }

    @Test
    void blankWeeklyOffDaysMeansNoWeeklyOffAtAll() {
        AttendanceRuleConfig config = defaultConfig();
        config.setWeeklyOffDays("");

        LocalDate sunday = LocalDate.of(2026, 1, 4); // confirmed Sunday above
        for (int i = 0; i < 7; i++) {
            assertFalse(engine.isWeeklyOff(config, sunday.plusDays(i)));
        }
    }
}
