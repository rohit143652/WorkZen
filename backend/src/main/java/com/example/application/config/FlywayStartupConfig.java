package com.example.application.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Runs Flyway exactly as before - with one safety check first (see DestructiveMigrationGuard). The check itself
 * can only ever ADD a refusal in the one dangerous situation; if it cannot work something out it stays out of
 * the way and migration proceeds normally, so it can never be the reason the application fails to start.
 */
@Configuration
public class FlywayStartupConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayStartupConfig.class);

    private static final List<String> TABLES_V109_WIPES = List.of(
            "attendance", "payroll_run_employees", "employee_advances", "leave_requests");

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(
            @Value("${app.allow-destructive-reset-migration:false}") boolean allowDestructiveReset) {
        return flyway -> {
            boolean block = false;
            Integer currentVersion = null;
            try {
                currentVersion = currentMajorVersion(flyway);
                if (currentVersion != null && currentVersion < DestructiveMigrationGuard.DESTRUCTIVE_VERSION) {
                    block = DestructiveMigrationGuard.shouldBlock(currentVersion, hasRealData(flyway), allowDestructiveReset);
                }
            } catch (RuntimeException guardProblem) {
                log.warn("Could not run the destructive-migration safety check ({}); continuing with the normal migration.", guardProblem.toString());
            }
            if (block) {
                throw new IllegalStateException("Refusing to start: this database is at schema version " + currentVersion
                        + " and already holds data, but migration V" + DestructiveMigrationGuard.DESTRUCTIVE_VERSION
                        + " would DELETE all payroll, attendance, leave and advance history and overwrite every employee's personal and bank details. "
                        + "Check you are connected to the right database (or restore a backup that is already at version "
                        + DestructiveMigrationGuard.DESTRUCTIVE_VERSION + " or later). If you really do want that wipe, set ALLOW_DESTRUCTIVE_RESET=true once.");
            }
            flyway.migrate();
        };
    }

    private static Integer currentMajorVersion(Flyway flyway) {
        MigrationInfo current = flyway.info().current();
        if (current == null || current.getVersion() == null) return null;
        try {
            return Integer.parseInt(current.getVersion().getVersion().split("\\.")[0]);
        } catch (NumberFormatException notNumeric) {
            return null;
        }
    }

    private static boolean hasRealData(Flyway flyway) {
        JdbcTemplate jdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
        for (String table : TABLES_V109_WIPES) {
            try {
                Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT 1 FROM " + table + " LIMIT 1) probe", Integer.class);
                if (found != null && found > 0) return true;
            } catch (DataAccessException tableNotThereYet) {
                // An old schema may not have this table at all - that simply means nothing to lose there.
            }
        }
        return false;
    }
}
