package com.example.application.calendar_module.dto;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * One shape for both Holiday and Event, since the Calendar UI renders them together (business
 * rule #18) - but they stay logically distinct: "type" tells the frontend which one it's
 * looking at, and the fields that don't apply to a given type are simply left null (e.g. a
 * HOLIDAY never has participantEmployeeIds; an EVENT with visibility=SELECTED_USERS never has
 * companyWide=true). This is a read-only projection for display - creating/editing still goes
 * through EventController or HolidayController directly, each with its own validation and
 * permission (EVENT_* vs HOLIDAY_*), matching business rule #30 (the two must stay separate in
 * backend authorization even though they share one DTO shape here).
 */
public class CalendarItemResponse {
    private Long id;
    private String type; // "EVENT" or "HOLIDAY"
    private String title;
    private String description;
    private String location;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean allDay;

    // EVENT-only
    private String visibility;
    private Set<Long> participantEmployeeIds;
    private String createdByName;

    // HOLIDAY-only
    private boolean companyWide;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public boolean isAllDay() { return allDay; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public Set<Long> getParticipantEmployeeIds() { return participantEmployeeIds; }
    public void setParticipantEmployeeIds(Set<Long> participantEmployeeIds) { this.participantEmployeeIds = participantEmployeeIds; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public boolean isCompanyWide() { return companyWide; }
    public void setCompanyWide(boolean companyWide) { this.companyWide = companyWide; }
}
