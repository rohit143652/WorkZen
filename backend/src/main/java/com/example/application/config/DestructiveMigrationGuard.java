package com.example.application.config;

/**
 * The decision, kept pure so it can be tested. V109 DELETES every company's payroll, attendance, leave and
 * advance history and rewrites every employee's personal/bank/statutory details with dummy values. Flyway runs
 * any migration a database has not seen yet - so pointing the application at an older database (or restoring a
 * pre-109 backup) that holds REAL data would wipe it on startup. This refuses that, unless explicitly allowed.
 */
public final class DestructiveMigrationGuard {

    /** The version of the migration that wipes data (V109__reset_dummy_data_july_to_date.sql). */
    public static final int DESTRUCTIVE_VERSION = 109;

    private DestructiveMigrationGuard() {
    }

    /**
     * @param currentVersion      the database's current schema version, or null for a brand-new database
     * @param databaseHasRealData whether the transactional tables already contain rows
     * @param overrideAllowed     the operator explicitly accepted the wipe
     */
    public static boolean shouldBlock(Integer currentVersion, boolean databaseHasRealData, boolean overrideAllowed) {
        if (overrideAllowed) return false;                         // explicitly accepted
        if (currentVersion == null) return false;                  // brand-new database: there is nothing to lose
        return currentVersion < DESTRUCTIVE_VERSION && databaseHasRealData;
    }
}
