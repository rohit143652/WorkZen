package com.example.application.leave_module.repository;

import com.example.application.leave_module.entity.PaidLeaveConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaidLeaveConfigurationRepository extends JpaRepository<PaidLeaveConfiguration, Long> {

    /**
     * The one row (if any) whose window covers the given date - ACTIVE only. Overlap prevention
     * (see PaidLeaveConfigService) guarantees at most one ACTIVE row can ever match a given date.
     */
    @Query("SELECT p FROM PaidLeaveConfiguration p WHERE p.clientCompanyId = :tenantId AND p.status = 'ACTIVE' "
            + "AND p.effectiveFrom <= :date AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date)")
    Optional<PaidLeaveConfiguration> findApplicableForDate(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    /** Every policy ever created for this tenant, newest first - for the Leave Policy History screen. */
    List<PaidLeaveConfiguration> findAllByClientCompanyIdOrderByEffectiveFromDesc(Long tenantId);

    /** ACTIVE rows only, ordered by start date - used to find the currently open-ended row when scheduling a new future policy. */
    List<PaidLeaveConfiguration> findAllByClientCompanyIdAndStatusOrderByEffectiveFromAsc(Long tenantId, String status);

    Optional<PaidLeaveConfiguration> findByIdAndClientCompanyId(Long id, Long tenantId);
}
