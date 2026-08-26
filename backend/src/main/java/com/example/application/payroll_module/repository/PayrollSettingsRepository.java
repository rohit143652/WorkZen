package com.example.application.payroll_module.repository;

import com.example.application.payroll_module.entity.PayrollSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollSettingsRepository extends JpaRepository<PayrollSettings, Long> {

    /**
     * The one row (if any) whose window covers the given date - i.e. effectiveFrom <= date AND
     * (effectiveTo IS NULL OR effectiveTo >= date), ACTIVE only. Overlap prevention (see
     * PayrollSettingsService) guarantees at most one ACTIVE row can ever match a given date, so
     * this is safe as a single-result query.
     */
    @Query("SELECT p FROM PayrollSettings p WHERE p.clientCompanyId = :tenantId AND p.status = 'ACTIVE' "
            + "AND p.effectiveFrom <= :date AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date)")
    Optional<PayrollSettings> findApplicableForDate(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    /** Every configuration ever created for this tenant, newest first - for the Settings History screen. */
    List<PayrollSettings> findAllByClientCompanyIdOrderByEffectiveFromDesc(Long tenantId);

    /** ACTIVE rows only, ordered by start date - used to find overlaps and to find "the currently open-ended row" when scheduling a new future configuration. */
    List<PayrollSettings> findAllByClientCompanyIdAndStatusOrderByEffectiveFromAsc(Long tenantId, String status);

    Optional<PayrollSettings> findByIdAndClientCompanyId(Long id, Long tenantId);
}
