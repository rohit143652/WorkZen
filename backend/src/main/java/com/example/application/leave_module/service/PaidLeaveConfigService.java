package com.example.application.leave_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.leave_module.dto.PaidLeaveConfigRequest;
import com.example.application.leave_module.dto.PaidLeaveConfigResponse;
import com.example.application.leave_module.entity.PaidLeaveConfiguration;
import com.example.application.leave_module.repository.PaidLeaveConfigurationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Leave policy CRUD (architecture refactor Phase 9, mirroring
 * payroll_module.PayrollSettingsService from Phase 8) - actual
 * month-by-month resolution for leave balance generation lives in
 * LeavePolicyResolver, not here; this class only manages the
 * historical/future timeline of policies an admin can see and edit.
 *
 * Same scheduling invariant as PayrollSettingsService keeps overlap
 * prevention simple and safe (spec section 3/5): at most one ACTIVE row
 * per tenant is ever "open" (effectiveTo IS NULL) at a time. Creating a
 * new policy always closes the current open one the day before the new
 * one starts; cancelling a not-yet-effective policy always reopens
 * whichever policy it had closed.
 */
@Service
public class PaidLeaveConfigService {

    private final PaidLeaveConfigurationRepository configRepository;
    private final LeavePolicyResolver leavePolicyResolver;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public PaidLeaveConfigService(PaidLeaveConfigurationRepository configRepository, LeavePolicyResolver leavePolicyResolver,
                                   TenantContextService tenantContext, AuditService auditService) {
        this.configRepository = configRepository;
        this.leavePolicyResolver = leavePolicyResolver;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    /** The policy in effect today - what the Settings screen shows as "Current". */
    @Transactional(readOnly = true)
    public PaidLeaveConfigResponse getForCurrentTenant() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return toResponse(leavePolicyResolver.resolve(tenantId, LocalDate.now().getYear(), LocalDate.now().getMonthValue()));
    }

    /** Used internally by EmployeePaidLeaveService's callers that don't already hold the resolver - kept as a documented delegation point. */
    @Transactional(readOnly = true)
    public PaidLeaveConfiguration getEntityOrDefault(Long tenantId) {
        return leavePolicyResolver.resolve(tenantId, LocalDate.now().getYear(), LocalDate.now().getMonthValue());
    }

    /** Full policy timeline for this tenant, newest first - for the Leave Policy History screen (spec section 32/33). */
    @Transactional(readOnly = true)
    public List<PaidLeaveConfigResponse> getHistory() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return configRepository.findAllByClientCompanyIdOrderByEffectiveFromDesc(tenantId).stream().map(this::toResponse).toList();
    }

    /** What would apply to a specific leave month, past or future. */
    @Transactional(readOnly = true)
    public PaidLeaveConfigResponse getForMonth(int year, int month) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return toResponse(leavePolicyResolver.resolve(tenantId, year, month));
    }

    /**
     * Schedules a new policy effective from the given date (today or later). Never mutates an
     * existing row's settings (spec section 16 - "policy change must not modify history") - it
     * only ever inserts a new row and, if there is a currently open-ended policy, closes it the
     * day before this one starts.
     */
    @Transactional
    public PaidLeaveConfigResponse createFutureConfig(PaidLeaveConfigRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        LocalDate effectiveFrom = request.getEffectiveFrom();
        if (effectiveFrom == null) {
            throw new BadRequestException("effectiveFrom is required");
        }

        PaidLeaveConfiguration currentOpen = findOpenConfig(tenantId).orElse(null);
        if (currentOpen != null) {
            if (!effectiveFrom.isAfter(currentOpen.getEffectiveFrom())) {
                throw new BadRequestException("New policy must be effective after the current policy's effective date ("
                        + currentOpen.getEffectiveFrom() + ") - overlapping policies are not allowed");
            }
            currentOpen.setEffectiveTo(effectiveFrom.minusDays(1));
            configRepository.save(currentOpen);
        }

        PaidLeaveConfiguration created = new PaidLeaveConfiguration();
        created.setClientCompanyId(tenantId);
        created.setEffectiveFrom(effectiveFrom);
        created.setStatus("ACTIVE");
        applyRequest(created, request);
        created.setCreatedBy(actorId);
        created.setUpdatedBy(actorId);
        PaidLeaveConfiguration saved = configRepository.save(created);

        auditService.log(actorId, "PAID_LEAVE_CONFIG_SCHEDULED",
                "Scheduled new leave policy effective " + effectiveFrom + ": " + saved.getMonthlyPaidLeave() + " day(s)/month, carry-forward "
                        + (saved.isAllowCarryForward() ? "allowed" : "not allowed")
                        + (saved.getMaximumCarryForward() != null ? " (max " + saved.getMaximumCarryForward() + ")" : " (unlimited)"),
                httpRequest);
        return toResponse(saved);
    }

    /** Edits a policy that has NOT yet taken effect - editing something already used by leave generation would be a destructive historical change. */
    @Transactional
    public PaidLeaveConfigResponse updateFutureConfig(Long id, PaidLeaveConfigRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PaidLeaveConfiguration config = getConfigForTenant(tenantId, id);
        assertNotYetEffective(config, "updated");
        applyRequest(config, request);
        config.setUpdatedBy(actorId);
        PaidLeaveConfiguration saved = configRepository.save(config);
        auditService.log(actorId, "PAID_LEAVE_CONFIG_UPDATED",
                "Updated not-yet-effective leave policy (effective " + saved.getEffectiveFrom() + ")", httpRequest);
        return toResponse(saved);
    }

    /** Cancelling a not-yet-effective policy reopens whichever policy it had closed, so there is never a coverage gap for future months. */
    @Transactional
    public void cancelFutureConfig(Long id, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        PaidLeaveConfiguration config = getConfigForTenant(tenantId, id);
        assertNotYetEffective(config, "cancelled");

        configRepository.findAllByClientCompanyIdAndStatusOrderByEffectiveFromAsc(tenantId, "ACTIVE").stream()
                .filter(other -> !other.getId().equals(config.getId()))
                .filter(other -> config.getEffectiveFrom().minusDays(1).equals(other.getEffectiveTo()))
                .findFirst()
                .ifPresent(previous -> {
                    previous.setEffectiveTo(null);
                    configRepository.save(previous);
                });

        config.setStatus("CANCELLED");
        config.setUpdatedBy(actorId);
        configRepository.save(config);
        auditService.log(actorId, "PAID_LEAVE_CONFIG_CANCELLED",
                "Cancelled the not-yet-effective leave policy scheduled for " + config.getEffectiveFrom(), httpRequest);
    }

    private Optional<PaidLeaveConfiguration> findOpenConfig(Long tenantId) {
        return configRepository.findAllByClientCompanyIdAndStatusOrderByEffectiveFromAsc(tenantId, "ACTIVE").stream()
                .filter(c -> c.getEffectiveTo() == null)
                .findFirst();
    }

    private PaidLeaveConfiguration getConfigForTenant(Long tenantId, Long id) {
        return configRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave policy " + id + " not found"));
    }

    private void assertNotYetEffective(PaidLeaveConfiguration config, String action) {
        if (!"ACTIVE".equals(config.getStatus())) {
            throw new BadRequestException("This policy is already cancelled");
        }
        if (!config.getEffectiveFrom().isAfter(LocalDate.now())) {
            throw new BadRequestException("Only a not-yet-effective (future) policy can be " + action
                    + " - this one is already effective and may already be used by leave balances");
        }
    }

    private void applyRequest(PaidLeaveConfiguration config, PaidLeaveConfigRequest request) {
        config.setMonthlyPaidLeave(request.getMonthlyPaidLeave());
        config.setEnabled(request.getEnabled());
        config.setAllowCarryForward(request.getAllowCarryForward());
        config.setMaximumCarryForward(request.getMaximumCarryForward());
        config.setResetAnnually(request.getResetAnnually());
    }

    private PaidLeaveConfigResponse toResponse(PaidLeaveConfiguration c) {
        PaidLeaveConfigResponse r = new PaidLeaveConfigResponse(c.getMonthlyPaidLeave(), c.isEnabled(), c.isAllowCarryForward(), c.getMaximumCarryForward(), c.isResetAnnually());
        r.setId(c.getId());
        r.setEffectiveFrom(c.getEffectiveFrom());
        r.setEffectiveTo(c.getEffectiveTo());
        r.setStatus(c.getStatus());
        return r;
    }
}
