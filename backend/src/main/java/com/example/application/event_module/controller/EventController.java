package com.example.application.event_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.event_module.dto.EventRequest;
import com.example.application.event_module.dto.EventResponse;
import com.example.application.event_module.service.EventService;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /** Used directly by CalendarController too (see CalendarService) - kept here as the one place event-visibility rules are applied. */
    @GetMapping
    @PreAuthorize("hasAuthority('EVENT_READ')")
    public ResponseEntity<ApiResponse<List<EventResponse>>> findInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("OK", eventService.findVisibleInRange(principal.getId(), start, end)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EVENT_CREATE')")
    public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody EventRequest request,
                                                               @AuthenticationPrincipal CustomUserPrincipal principal,
                                                               HttpServletRequest httpRequest) {
        EventResponse created = eventService.create(request, principal.getId(), httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.success("Event created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EVENT_UPDATE')")
    public ResponseEntity<ApiResponse<EventResponse>> update(@PathVariable Long id, @Valid @RequestBody EventRequest request,
                                                               @AuthenticationPrincipal CustomUserPrincipal principal,
                                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Event updated", eventService.update(id, request, principal.getId(), httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EVENT_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        eventService.delete(id, principal.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Event deleted", null));
    }
}
