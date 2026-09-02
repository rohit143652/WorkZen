package com.example.application.event_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.event_module.dto.EventRequest;
import com.example.application.event_module.dto.EventResponse;
import com.example.application.event_module.entity.Event;
import com.example.application.event_module.repository.EventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Events have a visibility model DIFFERENT from Holiday's always-company-wide rule (business
 * rule #30): ALL_USERS (every employee in the tenant) or SELECTED_USERS (only the specific
 * participants). Read access is filtered server-side in EventRepository.findVisibleInRange() -
 * there is no code path in this service that returns an event to someone who isn't allowed to
 * see it, matching business rule #19 (backend-enforced, not just hidden in the UI).
 */
@Service
public class EventService {

    private static final Set<String> VALID_VISIBILITY = Set.of("ALL_USERS", "SELECTED_USERS");

    private final EventRepository eventRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public EventService(EventRepository eventRepository, EmployeeRepository employeeRepository,
                         TenantContextService tenantContext, AuditService auditService) {
        this.eventRepository = eventRepository;
        this.employeeRepository = employeeRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    /** The employee's own visible events in a date/time range - used by the unified Calendar (day/week/month all just vary the range). */
    @Transactional(readOnly = true)
    public List<EventResponse> findVisibleInRange(Long viewerUserId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        // A login without a linked Employee profile (e.g. an admin-only account) still gets a
        // working Calendar - they just never match a SELECTED_USERS participant list, since
        // there's no employee identity to check against. They still see every ALL_USERS event
        // (and, separately, every Holiday - see CalendarService, which doesn't touch this at
        // all). Previously this threw and failed the WHOLE /api/calendar call (events AND
        // holidays both disappearing) just because the viewer happened to have no employee
        // record - that was the actual bug, not a permissions or deployment issue.
        Long viewerEmployeeId = employeeRepository.findByUserId(viewerUserId).map(Employee::getId).orElse(null);
        return eventRepository.findVisibleInRange(tenantId, viewerEmployeeId, rangeStart, rangeEnd).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public EventResponse create(EventRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        validate(request, tenantId);

        Event event = new Event();
        event.setClientCompanyId(tenantId);
        event.setCreatedBy(actorId);
        applyFields(event, request, actorId);
        Event saved = eventRepository.save(event);

        auditService.log(actorId, "EVENT_CREATED", "Created event \"" + saved.getTitle() + "\" (" + saved.getStartAt() + ")", httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Event event = eventRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        validate(request, tenantId);
        applyFields(event, request, actorId);
        Event saved = eventRepository.save(event);

        auditService.log(actorId, "EVENT_UPDATED", "Updated event \"" + saved.getTitle() + "\" (#" + saved.getId() + ")", httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Event event = eventRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        eventRepository.delete(event);
        auditService.log(actorId, "EVENT_DELETED", "Deleted event \"" + event.getTitle() + "\" (#" + event.getId() + ")", httpRequest);
    }

    private void validate(EventRequest request, Long tenantId) {
        if (!VALID_VISIBILITY.contains(request.getVisibility())) {
            throw new BadRequestException("Visibility must be one of: " + VALID_VISIBILITY);
        }
        if (request.getEndAt().isBefore(request.getStartAt())) {
            throw new BadRequestException("End time cannot be before start time.");
        }
        if ("SELECTED_USERS".equals(request.getVisibility())) {
            Set<Long> ids = request.getParticipantEmployeeIds();
            if (ids == null || ids.isEmpty()) {
                throw new BadRequestException("At least one participant is required when visibility is Selected Users.");
            }
            long validCount = employeeRepository.findAllById(ids).stream()
                    .filter(e -> e.getClientCompanyId().equals(tenantId)).count();
            if (validCount != ids.size()) {
                throw new BadRequestException("One or more selected participants are invalid.");
            }
        }
    }

    private void applyFields(Event event, EventRequest request, Long actorUserId) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setAllDay(request.isAllDay());
        event.setVisibility(request.getVisibility());

        if ("SELECTED_USERS".equals(request.getVisibility())) {
            Set<Long> participants = new HashSet<>(request.getParticipantEmployeeIds());
            // Whoever creates/edits a Selected-Users event should always be able to see it
            // themselves afterward, same as Google Calendar and most other calendar tools -
            // without this, someone who forgot to tick their own name would create an event
            // that immediately vanishes from their own view (the exact bug this fixes).
            employeeRepository.findByUserId(actorUserId).ifPresent(e -> participants.add(e.getId()));
            event.setParticipantEmployeeIds(participants);
        } else {
            event.setParticipantEmployeeIds(new HashSet<>());
        }
    }

    private EventResponse toResponse(Event e) {
        EventResponse r = new EventResponse();
        r.setId(e.getId());
        r.setTitle(e.getTitle());
        r.setDescription(e.getDescription());
        r.setLocation(e.getLocation());
        r.setStartAt(e.getStartAt());
        r.setEndAt(e.getEndAt());
        r.setAllDay(e.isAllDay());
        r.setVisibility(e.getVisibility());
        // Must be a fresh, plain HashSet copy - NOT the lazy Hibernate-managed collection
        // itself. Passing that reference straight through works fine right up until Jackson
        // serializes the response, which can happen after the transaction/session has already
        // closed - at that point touching the still-lazy collection throws
        // LazyInitializationException ("no Session"). Copying here, while the session is still
        // open, forces it to materialize immediately and produces a plain Set Jackson can always
        // serialize safely regardless of session state.
        r.setParticipantEmployeeIds(new java.util.HashSet<>(e.getParticipantEmployeeIds()));
        if (e.getCreatedBy() != null) {
            employeeRepository.findByUserId(e.getCreatedBy())
                    .ifPresent(emp -> r.setCreatedByName(emp.getFirstName() + " " + emp.getLastName()));
        }
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
