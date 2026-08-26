package com.example.application.leave_module.service;

import com.example.application.leave_module.entity.PaidLeaveConfiguration;
import com.example.application.leave_module.repository.PaidLeaveConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * THE single path that resolves "which paid-leave policy applies to this
 * client + this leave month" (architecture refactor Phase 9, mirroring
 * payroll_module.PayrollSettingsResolver from Phase 8). Fixes the exact
 * bug the original architecture audit flagged: EmployeePaidLeaveService
 * used to always read "today's" PaidLeaveConfiguration regardless of
 * which month it was resolving, so turning carry-forward ON/OFF today
 * could silently change how a PAST month's balance would look if ever
 * regenerated. Every caller now goes through resolve(tenantId, year,
 * month) instead.
 *
 * Resolution uses the FIRST calendar day of the leave month - a policy
 * effective from 01-Oct-2026 applies to October's balance (and every
 * month after, until superseded), never to September's.
 *
 * DEFAULT WHEN NOTHING HAS EVER BEEN CONFIGURED: Paid Leave is OFF
 * (enabled=false) - a client that has never explicitly scheduled a
 * policy grants no monthly paid leave at all, rather than silently
 * defaulting to 2 days/month. This is a synthetic, never-persisted
 * object (no id, no real effectiveFrom) - PaidLeaveConfigService.
 * toResponse() leaves effectiveFrom/id null for it, so the UI can tell
 * "nothing configured yet" apart from a real saved policy and avoid
 * showing a fabricated start date like 2000-01-01.
 */
@Service
public class LeavePolicyResolver {

    private final PaidLeaveConfigurationRepository configRepository;

    public LeavePolicyResolver(PaidLeaveConfigurationRepository configRepository) {
        this.configRepository = configRepository;
    }

    /** Never returns null - falls back to the "nothing configured, Paid Leave off" default (see class doc) when a tenant has no policy covering this month at all. */
    @Transactional(readOnly = true)
    public PaidLeaveConfiguration resolve(Long tenantId, int year, int month) {
        LocalDate monthStart = YearMonth.of(year, month).atDay(1);
        return configRepository.findApplicableForDate(tenantId, monthStart).orElseGet(() -> defaults(tenantId));
    }

    private PaidLeaveConfiguration defaults(Long tenantId) {
        PaidLeaveConfiguration defaults = new PaidLeaveConfiguration();
        defaults.setClientCompanyId(tenantId);
        defaults.setEnabled(false);
        defaults.setMonthlyPaidLeave(0);
        defaults.setAllowCarryForward(false);
        defaults.setMaximumCarryForward(null);
        // effectiveFrom deliberately left null - this object is never persisted and never
        // represents a real configured date; callers must not display it as one.
        return defaults;
    }
}
