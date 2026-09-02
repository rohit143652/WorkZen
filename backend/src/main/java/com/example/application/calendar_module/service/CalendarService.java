package com.example.application.calendar_module.service;

import com.example.application.calendar_module.dto.CalendarItemResponse;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.event_module.dto.EventResponse;
import com.example.application.event_module.service.EventService;
import com.example.application.holiday_module.entity.Holiday;
import com.example.application.holiday_module.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merges Events and Holidays into one list for the Calendar UI (business rule #18) while
 * keeping their visibility rules completely separate underneath (business rule #30):
 *   - Events go through EventService.findVisibleInRange(), which already enforces
 *     ALL_USERS/SELECTED_USERS visibility server-side.
 *   - Holidays are always company-wide (unchanged from the existing Holiday module - see
 *     HolidayRepository.findOverlapping()) - every employee in the tenant sees every holiday,
 *     with no participant/selection concept at all (business rule #4).
 * Viewing this combined calendar only requires EVENT_READ (granted broadly - see V94); Holiday
 * viewing has always been implicitly available to anyone who can see the calendar at all, while
 * creating/editing/deleting a Holiday remains separately gated by HOLIDAY_CREATE/HOLIDAY_DELETE
 * in HolidayController, untouched by this module.
 */
@Service
public class CalendarService {

    private final EventService eventService;
    private final HolidayRepository holidayRepository;
    private final TenantContextService tenantContext;

    public CalendarService(EventService eventService, HolidayRepository holidayRepository, TenantContextService tenantContext) {
        this.eventService = eventService;
        this.holidayRepository = holidayRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public List<CalendarItemResponse> findInRange(Long viewerUserId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        List<CalendarItemResponse> items = new ArrayList<>();

        for (EventResponse e : eventService.findVisibleInRange(viewerUserId, rangeStart, rangeEnd)) {
            CalendarItemResponse item = new CalendarItemResponse();
            item.setId(e.getId());
            item.setType("EVENT");
            item.setTitle(e.getTitle());
            item.setDescription(e.getDescription());
            item.setLocation(e.getLocation());
            item.setStartAt(e.getStartAt());
            item.setEndAt(e.getEndAt());
            item.setAllDay(e.isAllDay());
            item.setVisibility(e.getVisibility());
            item.setParticipantEmployeeIds(e.getParticipantEmployeeIds());
            item.setCreatedByName(e.getCreatedByName());
            items.add(item);
        }

        LocalDate holidayRangeStart = rangeStart.toLocalDate();
        LocalDate holidayRangeEnd = rangeEnd.toLocalDate();
        for (Holiday h : holidayRepository.findOverlapping(tenantId, holidayRangeStart, holidayRangeEnd)) {
            CalendarItemResponse item = new CalendarItemResponse();
            item.setId(h.getId());
            item.setType("HOLIDAY");
            item.setTitle(h.getName());
            item.setDescription(h.getDescription());
            // Holidays are date-only (no time-of-day concept) - always rendered in the all-day
            // section of Day/Week/Month (business rule #15), never as a timed 12:00 AM slot.
            item.setStartAt(h.getStartDate().atStartOfDay());
            item.setEndAt(h.getEndDate().atTime(LocalTime.MAX));
            item.setAllDay(true);
            item.setCompanyWide(true);
            items.add(item);
        }

        return items.stream().sorted(Comparator.comparing(CalendarItemResponse::getStartAt)).toList();
    }
}
