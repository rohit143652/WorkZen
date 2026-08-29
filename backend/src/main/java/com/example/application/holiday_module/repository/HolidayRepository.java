package com.example.application.holiday_module.repository;

import com.example.application.holiday_module.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findAllByClientCompanyIdOrderByStartDateDesc(Long clientCompanyId);

    Optional<Holiday> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    /** Any existing holiday whose range overlaps [start, end] at all - standard "two ranges overlap" test: NOT (existing ends before new starts, OR existing starts after new ends). */
    @Query("SELECT h FROM Holiday h WHERE h.clientCompanyId = :tenantId AND h.startDate <= :end AND h.endDate >= :start")
    List<Holiday> findOverlapping(@Param("tenantId") Long tenantId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
