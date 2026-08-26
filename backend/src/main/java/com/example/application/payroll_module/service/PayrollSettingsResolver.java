package com.example.application.payroll_module.service;

import com.example.application.payroll_module.entity.PayrollSettings;
import com.example.application.payroll_module.repository.PayrollSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * THE single path that resolves "which PayrollSettings configuration
 * applies to this client + this payroll month" (architecture refactor
 * Phase 8, spec section 17). PayrollRunService is the only caller -
 * PayrollCalculationService itself never queries PayrollSettings, it only
 * receives the already-resolved rates as part of PayrollCalculationInput
 * (unchanged since Phase 1).
 *
 * Resolution uses the FIRST calendar day of the payroll month - i.e. a
 * configuration effective from 01-Oct-2026 applies to the October payroll
 * run (and every month after, until superseded), never to September's,
 * even though October's calculation may happen well into the month.
 */
@Service
public class PayrollSettingsResolver {

    private final PayrollSettingsRepository settingsRepository;

    public PayrollSettingsResolver(PayrollSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /**
     * Never returns null - falls back to documented defaults (spec section 27: "if current code
     * has defaults, reuse them") when a tenant has no configuration covering this month at all,
     * e.g. a brand-new tenant that has never opened the Payroll Settings screen.
     */
    @Transactional(readOnly = true)
    public PayrollSettings resolve(Long tenantId, int year, int month) {
        LocalDate monthStart = YearMonth.of(year, month).atDay(1);
        return settingsRepository.findApplicableForDate(tenantId, monthStart).orElseGet(() -> defaults(tenantId));
    }

    /** Same defaults the old single-row model shipped with - EPF 12/13%, ESI 0.75/3.25% under a 21,000 ceiling, PT 200 flat. Not persisted - only returned to the caller for this one resolution. */
    private PayrollSettings defaults(Long tenantId) {
        PayrollSettings defaults = new PayrollSettings();
        defaults.setClientCompanyId(tenantId);
        defaults.setEffectiveFrom(LocalDate.of(2000, 1, 1));
        return defaults; // entity field initializers already hold the documented defaults
    }
}
