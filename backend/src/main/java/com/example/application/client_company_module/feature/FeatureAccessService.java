package com.example.application.client_company_module.feature;

import com.example.application.common.tenant.TenantContextService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Company Feature Configuration is the UPPER-LEVEL control, checked in addition to (never
 * instead of) the existing @PreAuthorize role/permission checks - see class javadoc on
 * AttendanceService for how the two combine: company feature enabled AND role permission
 * allowed, both required, company restriction always wins if either says no.
 *
 * Effective access priority (subscription plan system):
 *   1. Subscription status - SUSPENDED/CANCELLED blocks everything regardless of feature/plan
 *      (see SubscriptionAccessPolicy, called separately by controllers/filters that need it).
 *   2. Explicit CompanyFeature override for this (tenant, code) - if a row exists, its enabled
 *      value wins outright, whichever way it's set. This is UNCHANGED from before the
 *      subscription system existed - every row that already existed keeps meaning exactly what
 *      it always meant.
 *   3. No override row -> the tenant's subscribed PLAN's default for that code (PlanFeature).
 *   4. No subscription/plan resolvable at all (should not happen once V110's backfill has run,
 *      but fails open rather than locking a tenant out over a data gap) -> enabled, matching the
 *      original pre-subscription behavior exactly for that edge case only.
 *
 * Before the subscription system existed, step 3 didn't exist and step 4 was the ONLY fallback
 * (absence of a row always meant enabled) - safe only because there was no paid tier for that to
 * silently bypass. Now that plans exist, a feature a tenant's plan doesn't include must actually
 * be off by default, not on just because nobody explicitly disabled it (see V110 migration).
 */
@Service
public class FeatureAccessService {

    private final CompanyFeatureRepository repository;
    private final TenantContextService tenantContext;
    private final com.example.application.subscription_module.repository.ClientSubscriptionRepository subscriptionRepository;
    private final com.example.application.subscription_module.repository.PlanFeatureRepository planFeatureRepository;

    public FeatureAccessService(CompanyFeatureRepository repository, TenantContextService tenantContext,
                                 com.example.application.subscription_module.repository.ClientSubscriptionRepository subscriptionRepository,
                                 com.example.application.subscription_module.repository.PlanFeatureRepository planFeatureRepository) {
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.subscriptionRepository = subscriptionRepository;
        this.planFeatureRepository = planFeatureRepository;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(Long clientCompanyId, String featureCode) {
        return repository.findByClientCompanyIdAndFeatureCode(clientCompanyId, featureCode)
                .map(CompanyFeature::isEnabled)
                .orElseGet(() -> planDefaultFor(clientCompanyId, featureCode));
    }

    /** Step 3/4 of the priority above - the tenant's subscribed plan's default for this code, or true if no subscription/plan can be resolved at all (data-gap fallback, see class javadoc). */
    private boolean planDefaultFor(Long clientCompanyId, String featureCode) {
        return subscriptionRepository.findByClientCompanyId(clientCompanyId)
                .map(sub -> planFeatureRepository.findByPlanIdAndFeatureCode(sub.getPlanId(), featureCode)
                        .map(pf -> pf.isEnabled())
                        .orElse(false)) // plan resolved but doesn't include this code -> genuinely off
                .orElse(true); // no subscription row at all -> fail open, see class javadoc
    }

    @Transactional(readOnly = true)
    public boolean isEnabledForCurrentTenant(String featureCode) {
        Long tenantId = tenantContext.currentTenantIdOrNull();
        // No tenant context at all (a SUPER_ADMIN/global-scope call) - feature restrictions are
        // a per-company concept, so there's nothing to restrict here; let it through.
        if (tenantId == null) return true;
        return isEnabled(tenantId, featureCode);
    }

    /**
     * Throws a clean, specific AccessDeniedException (caught by the same CustomAccessDeniedHandler
     * every other 403 in this app goes through) if the current tenant doesn't have this feature -
     * the reusable enforcement point every protected service method should call first, matching
     * the spec's own example error shape ("X is not enabled for this company.").
     */
    @Transactional(readOnly = true)
    public void requireEnabledForCurrentTenant(String featureCode, String featureLabel) {
        if (!isEnabledForCurrentTenant(featureCode)) {
            throw new AccessDeniedException(featureLabel + " is not enabled for this company. Contact your Super Admin.");
        }
    }

    /** Every known feature code (see FeatureCode.CATALOG) mapped to whether it's currently EFFECTIVELY enabled for this company (override if present, else plan default) - for the Manage Features screen and the frontend's "my enabled features" call. */
    @Transactional(readOnly = true)
    public Map<String, Boolean> getFeatureMap(Long clientCompanyId) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (FeatureCode.FeatureCategory category : FeatureCode.CATALOG) {
            for (String code : category.codes()) {
                result.put(code, isEnabled(clientCompanyId, code));
            }
        }
        return result;
    }

    /** Richer version for the Manage Features / Client Feature Overrides screen - shows the plan default, the client override (if any), and the effective result side by side, per spec: "Feature: PAYROLL, Plan: Enabled, Client Override: No Override, Effective: Enabled". */
    @Transactional(readOnly = true)
    public List<FeatureEffectiveStatus> getEffectiveFeatureDetails(Long clientCompanyId) {
        List<FeatureEffectiveStatus> result = new java.util.ArrayList<>();
        for (FeatureCode.FeatureCategory category : FeatureCode.CATALOG) {
            for (String code : category.codes()) {
                boolean planDefault = planDefaultFor(clientCompanyId, code);
                Boolean override = repository.findByClientCompanyIdAndFeatureCode(clientCompanyId, code)
                        .map(CompanyFeature::isEnabled).orElse(null);
                boolean effective = override != null ? override : planDefault;
                result.add(new FeatureEffectiveStatus(code, category.label(), planDefault, override, effective));
            }
        }
        return result;
    }

    /** One feature code's plan default / client override (null = no override) / effective result, for the Manage Features screen. */
    public record FeatureEffectiveStatus(String featureCode, String category, boolean planDefault, Boolean clientOverride, boolean effective) {}

    /** Super Admin updating one company's feature flags - upserts each code individually so a partial update (only some codes present) never touches the others. */
    @Transactional
    public Map<String, Boolean> updateFeatures(Long clientCompanyId, Map<String, Boolean> updates, Long actorId) {
        for (Map.Entry<String, Boolean> entry : updates.entrySet()) {
            CompanyFeature cf = repository.findByClientCompanyIdAndFeatureCode(clientCompanyId, entry.getKey())
                    .orElseGet(() -> {
                        CompanyFeature fresh = new CompanyFeature();
                        fresh.setClientCompanyId(clientCompanyId);
                        fresh.setFeatureCode(entry.getKey());
                        return fresh;
                    });
            cf.setEnabled(entry.getValue());
            cf.setUpdatedBy(actorId);
            repository.save(cf);
        }
        return getFeatureMap(clientCompanyId);
    }

    /** Feature codes that must be an explicit Super Admin opt-in even for a brand-new company -
        true-by-default (matching the "absence of a row = enabled" rule) is right for most
        features, but wrong for OVERTIME_MANAGEMENT specifically, since it's a real payroll-
        calculation change, not a passive module (see V106 migration). */
    private static final java.util.Set<String> OPT_IN_DEFAULT_OFF = java.util.Set.of(FeatureCode.OVERTIME_MANAGEMENT);

    /** Seeds every catalog feature code for a brand-new tenant - called once from ClientCompanyService.create() so a new company's Manage Features screen has concrete rows to show immediately, same as the V102/V106 migrations did for existing tenants. */
    @Transactional
    public void seedDefaultsForNewCompany(Long clientCompanyId) {
        for (FeatureCode.FeatureCategory category : FeatureCode.CATALOG) {
            for (String code : category.codes()) {
                if (repository.findByClientCompanyIdAndFeatureCode(clientCompanyId, code).isEmpty()) {
                    CompanyFeature cf = new CompanyFeature();
                    cf.setClientCompanyId(clientCompanyId);
                    cf.setFeatureCode(code);
                    cf.setEnabled(!OPT_IN_DEFAULT_OFF.contains(code));
                    repository.save(cf);
                }
            }
        }
    }
}
