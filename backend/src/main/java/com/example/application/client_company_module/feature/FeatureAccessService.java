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
 * Absence of a company_feature row for a given (tenant, code) means ENABLED, not disabled - see
 * V102's migration comment for why: a feature code added after a tenant was created should never
 * silently lock that tenant out just because nobody has explicitly configured it yet. A tenant
 * only loses access once a Super Admin has explicitly flipped it off.
 */
@Service
public class FeatureAccessService {

    private final CompanyFeatureRepository repository;
    private final TenantContextService tenantContext;

    public FeatureAccessService(CompanyFeatureRepository repository, TenantContextService tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(Long clientCompanyId, String featureCode) {
        return repository.findByClientCompanyIdAndFeatureCode(clientCompanyId, featureCode)
                .map(CompanyFeature::isEnabled)
                .orElse(true);
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

    /** Every known feature code (see FeatureCode.CATALOG) mapped to whether it's currently enabled for this company - for the Manage Features screen and the frontend's "my enabled features" call. */
    @Transactional(readOnly = true)
    public Map<String, Boolean> getFeatureMap(Long clientCompanyId) {
        Map<String, Boolean> overrides = new LinkedHashMap<>();
        for (CompanyFeature cf : repository.findAllByClientCompanyId(clientCompanyId)) {
            overrides.put(cf.getFeatureCode(), cf.isEnabled());
        }
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (FeatureCode.FeatureCategory category : FeatureCode.CATALOG) {
            for (String code : category.codes()) {
                result.put(code, overrides.getOrDefault(code, true));
            }
        }
        return result;
    }

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
