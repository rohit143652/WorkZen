package com.example.application.common.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/** "Today" must be the business's date, not the server's - the whole point of AppTime. */
class AppTimeTest {

    @Test
    void theApplicationClockReadsTheBusinessZoneNotTheServersZone() {
        LocalDateTime inBusinessZone = LocalDateTime.now(AppTime.zone());
        assertTrue(Math.abs(Duration.between(inBusinessZone, AppTime.now()).toSeconds()) <= 2,
                "AppTime.now() must be the wall clock of AppTime.zone(), whatever zone the JVM runs in");
    }

    @Test
    void todayIsAlwaysTheDatePartOfNow() {
        // (can only differ if the test straddles midnight, which the retry below tolerates)
        LocalDate today = AppTime.today();
        LocalDate fromNow = AppTime.now().toLocalDate();
        assertTrue(today.equals(fromNow) || today.plusDays(1).equals(fromNow));
    }

    @Test
    void theDefaultZoneIsIndiaUnlessConfigured() {
        if (System.getenv("APP_TIMEZONE") == null && System.getProperty("app.timezone") == null) {
            assertEquals(ZoneId.of("Asia/Kolkata"), AppTime.zone());
        }
    }
}
