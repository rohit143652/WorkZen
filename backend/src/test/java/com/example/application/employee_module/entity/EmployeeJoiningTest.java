package com.example.application.employee_module.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** The single rule every month-based report and payroll uses to decide whether someone belongs in a given month. */
class EmployeeJoiningTest {

    private static Employee joinedOn(LocalDate date) {
        Employee e = new Employee();
        e.setJoiningDate(date);
        return e;
    }

    private static final LocalDate JULY_END = LocalDate.of(2026, 7, 31);

    @Test
    void someoneWhoJoinsInAugustHasNotJoinedByTheEndOfJuly() {
        assertFalse(joinedOn(LocalDate.of(2026, 8, 1)).hasJoinedBy(JULY_END));
        assertFalse(joinedOn(LocalDate.of(2026, 8, 20)).hasJoinedBy(JULY_END));
    }

    @Test
    void someoneWhoJoinsOnAnyDayOfAMonthIsInThatMonthEvenTheLastDay() {
        assertTrue(joinedOn(LocalDate.of(2026, 7, 31)).hasJoinedBy(JULY_END));
        assertTrue(joinedOn(LocalDate.of(2026, 7, 1)).hasJoinedBy(JULY_END));
        assertTrue(joinedOn(LocalDate.of(2026, 7, 15)).hasJoinedBy(JULY_END));
    }

    @Test
    void someoneWhoJoinedInAnEarlierMonthIsInEveryLaterMonth() {
        assertTrue(joinedOn(LocalDate.of(2026, 6, 10)).hasJoinedBy(JULY_END));
        assertTrue(joinedOn(LocalDate.of(2025, 1, 1)).hasJoinedBy(JULY_END));
    }

    @Test
    void aMissingJoiningDateCountsAsJoinedSoIncompleteDataNeverMakesSomeoneVanishFromPayroll() {
        assertTrue(joinedOn(null).hasJoinedBy(JULY_END));
    }
}
