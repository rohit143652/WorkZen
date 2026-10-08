package com.example.application.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The application's notion of "today" and "now", always in the business's own time zone
 * (default Asia/Kolkata) - NOT whatever zone the server happens to run in.
 *
 * Servers (AWS especially) default to UTC, which is 5h30m behind India: LocalDate.now() would still say
 * "yesterday" until 05:30 IST, so an early-shift check-in landed on the wrong day and late / early-exit
 * minutes were computed against a clock 5h30m off. Every date/time the application itself decides
 * ("today", "check-in time", "audit time") goes through here instead.
 *
 * This only chooses WHICH wall-clock reading is used. It deliberately does not touch how Hibernate
 * stores dates (hibernate.jdbc.time_zone) - changing that would re-interpret every existing row.
 *
 * Override with the APP_TIMEZONE environment variable (or -Dapp.timezone=...) for a business outside India.
 */
public final class AppTime {

    private static final String DEFAULT_ZONE = "Asia/Kolkata";
    private static final ZoneId ZONE = resolve();

    private AppTime() {
    }

    private static ZoneId resolve() {
        String configured = System.getenv("APP_TIMEZONE");
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("app.timezone", DEFAULT_ZONE);
        }
        try {
            return ZoneId.of(configured.trim());
        } catch (RuntimeException invalid) {
            // A mistyped zone must not stop the application from starting; fall back to India time.
            return ZoneId.of(DEFAULT_ZONE);
        }
    }

    public static ZoneId zone() {
        return ZONE;
    }

    /** Today's date in the business time zone. */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** The current date and time in the business time zone. */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
