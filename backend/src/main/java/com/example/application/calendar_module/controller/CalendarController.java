package com.example.application.calendar_module.controller;

import com.example.application.calendar_module.dto.CalendarItemResponse;
import com.example.application.calendar_module.service.CalendarService;
import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.security.CustomUserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ONE endpoint the Day/Week/Month calendar views all call, just varying the [start, end) range -
 * see CalendarService for how Events and Holidays are merged while keeping their visibility
 * rules separate. Creating/editing an Event or Holiday still goes through EventController /
 * HolidayController directly (each with its own permission), never through here.
 */
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EVENT_READ')")
    public ResponseEntity<ApiResponse<List<CalendarItemResponse>>> findInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("OK", calendarService.findInRange(principal.getId(), start, end)));
    }
}
