package com.example.application.event_module.repository;

import com.example.application.event_module.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    /**
     * Only returns events the given employee is actually allowed to see - either ALL_USERS
     * (visible tenant-wide) or SELECTED_USERS where this employee is a participant. This is the
     * ONE place calendar visibility is decided; every caller (day/week/month calendar views)
     * goes through this so there is no code path that fetches everyone's events and relies on
     * the frontend to hide the ones a user shouldn't see (business rule #19).
     */
    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN e.participantEmployeeIds p " +
            "WHERE e.clientCompanyId = :tenantId " +
            "AND e.startAt < :rangeEnd AND e.endAt > :rangeStart " +
            "AND (e.visibility = 'ALL_USERS' OR p = :employeeId) " +
            "ORDER BY e.startAt ASC")
    List<Event> findVisibleInRange(@Param("tenantId") Long tenantId, @Param("employeeId") Long employeeId,
                                    @Param("rangeStart") LocalDateTime rangeStart, @Param("rangeEnd") LocalDateTime rangeEnd);
}
