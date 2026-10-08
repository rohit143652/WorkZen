package com.example.application.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The rule that stops the wipe in V109 from running over a live database. */
class DestructiveMigrationGuardTest {

    @Test
    void aBrandNewDatabaseIsNeverBlocked() {
        assertFalse(DestructiveMigrationGuard.shouldBlock(null, false, false));
        assertFalse(DestructiveMigrationGuard.shouldBlock(null, true, false));
    }

    @Test
    void anOlderDatabaseThatAlreadyHoldsDataIsBlocked() {
        assertTrue(DestructiveMigrationGuard.shouldBlock(108, true, false));
        assertTrue(DestructiveMigrationGuard.shouldBlock(1, true, false));
    }

    @Test
    void anOlderDatabaseWithNothingInItIsNotBlocked() {
        assertFalse(DestructiveMigrationGuard.shouldBlock(108, false, false));
    }

    @Test
    void aDatabaseAlreadyPastV109IsNeverBlockedBecauseV109NeverRunsAgain() {
        assertFalse(DestructiveMigrationGuard.shouldBlock(109, true, false));
        assertFalse(DestructiveMigrationGuard.shouldBlock(124, true, false));
    }

    @Test
    void anExplicitOverrideAllowsTheWipe() {
        assertFalse(DestructiveMigrationGuard.shouldBlock(108, true, true));
    }
}
