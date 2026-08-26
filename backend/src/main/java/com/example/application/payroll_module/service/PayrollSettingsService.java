package com.example.application.payroll_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.payroll_module.dto.PayrollSettingsRequest;
import com.example.application.payroll_module.dto.PayrollSettingsResponse;
import com.example.application.payroll_module.entity.PayrollSettings;
import com.example.application.payroll_module.repository.PayrollSettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Payroll configuration CRUD (architecture refactor Phase 8) - actual
 * month-by-month resolution for payroll calculation lives in
 * PayrollSettingsResolver, not here; this class only manages the
 * historical/future timeline of configurations an admin can see and edit.
 *
 * Scheduling invariant that keeps overlap-prevention simple and safe
 * (spec section 5): at most one ACTIVE row per tenant is ever "open"
 * (effectiveTo IS NULL) at a time. Creating a new configuration always
 * closes the current open one the day before the new one starts;
 * cancelling a not-yet-effective configuration always reopens whichever
 * configuration it had closed. Every write here goes through this same
 * invariant, so two ACTIVE configurations for one tenant can never overlap.
 */
@Service
public class PayrollSettingsService {

    private final PayrollSettingsRepository settingsRepository;
    private final PayrollSettingsResolver settingsResolver;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public PayrollSettingsService(PayrollSettingsRepository settingsRepository, PayrollSettingsResolver settingsResolver,
                                   TenantContextService tenantContext, AuditService auditService) {
        this.settingsRepository = settingsRepository;
        this.settingsResolver = settingsResolver;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    /** The configuration in effect today - what the Settings screen shows as "Current". */
    @Transactional(readOnly = true)
    public PayrollSettingsResponse getForCurrentTenant() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return toResponse(settingsResolver.resolve(tenantId, LocalDate.now().getYear(), LocalDate.now().getMonthValue()));
    }

    /** Used internally by PayrollRunService for month-accurate resolution - see PayrollSettingsResolver.resolve(). Kept here only as a documented delegation point so callers don't need to know the resolver exists separately. */
    @Transactional(readOnly = true)
    public PayrollSettings getEntityOrDefault(Long tenantId) {
        return settingsResolver.resolve(tenantId, LocalDate.now().getYear(), LocalDate.now().getMonthValue());
    }

    /** Full configuration timeline for this tenant, newest first - for the Settings History screen (spec section 8/30). */
    @Transactional(readOnly = true)
    public List<PayrollSettingsResponse> getHistory() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return settingsRepository.findAllByClientCompanyIdOrderByEffectiveFromDesc(tenantId).stream().map(this::toResponse).toList();
    }

    /** What would apply to a specific payroll month - lets the Settings screen show "Configuration Used" for any month, past or future (spec section 31/7). */
    @Transactional(readOnly = true)
    public PayrollSettingsResponse getForMonth(int year, int month) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return toResponse(settingsResolver.resolve(tenantId, year, month));
    }

    /**
     * Schedules a new configuration effective from the given date (today or later). Never
     * mutates an existing row's percentages (spec section 23 - "no destructive update") - it
     * only ever inserts a new row and, if there is a currently open-ended configuration, closes
     * it the day before this one starts, preserving the "at most one open row" invariant.
     */
    @Transactional
    public PayrollSettingsResponse createFutureConfig(PayrollSettingsRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        LocalDate effectiveFrom = request.getEffectiveFrom();
        if (effectiveFrom == null) {
            throw new BadRequestException("effectiveFrom is required");
        }
        validatePercentages(request);

        PayrollSettings currentOpen = findOpenConfig(tenantId).orElse(null);
        if (currentOpen != null) {
            if (!effectiveFrom.isAfter(currentOpen.getEffectiveFrom())) {
                throw new BadRequestException("New configuration must be effective after the current configuration's effective date ("
                        + currentOpen.getEffectiveFrom() + ") - overlapping configurations are not allowed");
            }
            currentOpen.setEffectiveTo(effectiveFrom.minusDays(1));
            settingsRepository.save(currentOpen);
        }

        PayrollSettings created = new PayrollSettings();
        created.setClientCompanyId(tenantId);
        created.setEffectiveFrom(effectiveFrom);
        created.setStatus("ACTIVE");
        applyRequest(created, request);
        created.setCreatedBy(actorId);
        created.setUpdatedBy(actorId);
        PayrollSettings saved = settingsRepository.save(created);

        auditService.log(actorId, "PAYROLL_SETTINGS_SCHEDULED",
                "Scheduled new payroll configuration effective " + effectiveFrom
                        + " (EPF " + request.getEpfEmployeePercent() + "%, ESI " + request.getEsiEmployeePercent() + "%, PT " + request.getProfessionalTax() + ")",
                httpRequest);
        return toResponse(saved);
    }

    /**
     * Edits a configuration that has NOT yet taken effect (spec section 24 - editing something
     * already used by payroll would be a destructive historical change). Effective date itself
     * cannot be changed here (that would risk re-opening an overlap the invariant already
     * closed) - cancel and recreate instead if the date needs to move.
     */
    @Transactional
    public PayrollSettingsResponse updateFutureConfig(Long id, PayrollSettingsRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollSettings settings = getConfigForTenant(tenantId, id);
        assertNotYetEffective(settings, "updated");
        validatePercentages(request);
        applyRequest(settings, request);
        settings.setUpdatedBy(actorId);
        PayrollSettings saved = settingsRepository.save(settings);
        auditService.log(actorId, "PAYROLL_SETTINGS_UPDATED",
                "Updated not-yet-effective payroll configuration (effective " + settings.getEffectiveFrom() + ")", httpRequest);
        return toResponse(saved);
    }

    /** Cancelling a not-yet-effective configuration reopens whichever configuration it had closed, so there is never a coverage gap for future months. */
    @Transactional
    public void cancelFutureConfig(Long id, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PayrollSettings settings = getConfigForTenant(tenantId, id);
        assertNotYetEffective(settings, "cancelled");

        settingsRepository.findAllByClientCompanyIdAndStatusOrderByEffectiveFromAsc(tenantId, "ACTIVE").stream()
                .filter(other -> !other.getId().equals(settings.getId()))
                .filter(other -> settings.getEffectiveFrom().minusDays(1).equals(other.getEffectiveTo()))
                .findFirst()
                .ifPresent(previous -> {
                    previous.setEffectiveTo(null);
                    settingsRepository.save(previous);
                });

        settings.setStatus("CANCELLED");
        settings.setUpdatedBy(actorId);
        settingsRepository.save(settings);
        auditService.log(actorId, "PAYROLL_SETTINGS_CANCELLED",
                "Cancelled the not-yet-effective payroll configuration scheduled for " + settings.getEffectiveFrom(), httpRequest);
    }

    private Optional<PayrollSettings> findOpenConfig(Long tenantId) {
        return settingsRepository.findAllByClientCompanyIdAndStatusOrderByEffectiveFromAsc(tenantId, "ACTIVE").stream()
                .filter(s -> s.getEffectiveTo() == null)
                .findFirst();
    }

    private PayrollSettings getConfigForTenant(Long tenantId, Long id) {
        return settingsRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll configuration " + id + " not found"));
    }

    private void assertNotYetEffective(PayrollSettings settings, String action) {
        if (!"ACTIVE".equals(settings.getStatus())) {
            throw new BadRequestException("This configuration is already cancelled");
        }
        if (!settings.getEffectiveFrom().isAfter(LocalDate.now())) {
            throw new BadRequestException("Only a not-yet-effective (future) configuration can be " + action
                    + " - this one is already effective and may already be used by payroll");
        }
    }

    private void validatePercentages(PayrollSettingsRequest request) {
        BigDecimalRange.assertPercent(request.getEpfEmployeePercent(), "epfEmployeePercent");
        BigDecimalRange.assertPercent(request.getEpfEmployerPercent(), "epfEmployerPercent");
        BigDecimalRange.assertPercent(request.getEsiEmployeePercent(), "esiEmployeePercent");
        BigDecimalRange.assertPercent(request.getEsiEmployerPercent(), "esiEmployerPercent");
    }

    private void applyRequest(PayrollSettings settings, PayrollSettingsRequest request) {
        settings.setEpfEnabled(request.getEpfEnabled());
        settings.setEpfEmployeePercent(request.getEpfEmployeePercent());
        settings.setEpfEmployerPercent(request.getEpfEmployerPercent());
        settings.setEsiEnabled(request.getEsiEnabled());
        settings.setEsiEmployeePercent(request.getEsiEmployeePercent());
        settings.setEsiEmployerPercent(request.getEsiEmployerPercent());
        settings.setEsiWageCeiling(request.getEsiWageCeiling());
        settings.setPtEnabled(request.getPtEnabled());
        settings.setProfessionalTax(request.getProfessionalTax());
    }

    private PayrollSettingsResponse toResponse(PayrollSettings s) {
        PayrollSettingsResponse r = new PayrollSettingsResponse(s.isEpfEnabled(), s.getEpfEmployeePercent(), s.getEpfEmployerPercent(),
                s.isEsiEnabled(), s.getEsiEmployeePercent(), s.getEsiEmployerPercent(), s.getEsiWageCeiling(),
                s.isPtEnabled(), s.getProfessionalTax());
        r.setId(s.getId());
        r.setEffectiveFrom(s.getEffectiveFrom());
        r.setEffectiveTo(s.getEffectiveTo());
        r.setStatus(s.getStatus());
        return r;
    }

    /** Small inline guard so 0-100 range validation isn't scattered/repeated - not a full separate validation framework. */
    private static final class BigDecimalRange {
        static void assertPercent(java.math.BigDecimal value, String field) {
            if (value == null || value.signum() < 0 || value.compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new BadRequestException(field + " must be between 0 and 100");
            }
        }
    }
}
