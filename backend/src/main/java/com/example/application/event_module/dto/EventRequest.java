package com.example.application.event_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

public class EventRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String location;

    @NotNull(message = "Start date/time is required")
    private LocalDateTime startAt;

    @NotNull(message = "End date/time is required")
    private LocalDateTime endAt;

    private boolean allDay;

    /** ALL_USERS or SELECTED_USERS - see Event entity javadoc. */
    @NotBlank(message = "Visibility is required")
    private String visibility;

    /** Only meaningful (and required to be non-empty) when visibility = SELECTED_USERS - ignored for ALL_USERS. */
    private Set<Long> participantEmployeeIds;

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
}
