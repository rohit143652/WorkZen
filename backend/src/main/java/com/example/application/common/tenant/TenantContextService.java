package com.example.application.common.tenant;

import com.example.application.common.exception.BadRequestException;
import com.example.application.login_module.security.CustomUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * The single, authoritative source for "which tenant is this request
 * operating as". Every tenant-scoped service MUST go through this class
 * rather than reading a clientCompanyId off a request DTO - request bodies
 * for tenant-scoped endpoints deliberately have no such field at all (see
 * EmployeeRequest, SiteRequest), so there is nothing for
 * a malicious client to override.
 *
 * SUPER_ADMIN has no single tenant (users.client_company_id IS NULL) and is
 * allowed to explicitly target any Client Company via a path variable on
 * SUPER_ADMIN-only endpoints (e.g. GET /api/client-companies/{id}); that is
 * an explicit, permission-gated action, never an implicit default.
 */
@Service
public class TenantContextService {

    public CustomUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            throw new IllegalStateException("No authenticated principal in the current security context");
        }
        return principal;
    }

    public boolean isSuperAdmin() {
        return currentPrincipal().getRoleNames().contains("SUPER_ADMIN");
    }

    /**
     * The current user's own effective permission set. Used as a ceiling by
     * RoleService: a non-SUPER_ADMIN can never grant a role a permission
     * they do not themselves hold, closing the privilege-escalation path
     * where a Client Admin could otherwise create a custom role bundled
     * with a SUPER_ADMIN-only permission and hand it to an employee.
     */
    public java.util.Set<String> currentPermissionNames() {
        return currentPrincipal().getPermissionNames();
    }

    /** Null for SUPER_ADMIN / any house user. Non-null for tenant-scoped users. */
    public Long currentTenantIdOrNull() {
        return currentPrincipal().getUser().getClientCompanyId();
    }

    /**
     * For endpoints that only make sense within a single tenant (creating an
     * employee, a site, a sub-client...). Throws if the caller has no tenant -
     * i.e. a SUPER_ADMIN calling a tenant-only-creation endpoint, which is a
     * deliberate 400 rather than silently picking a tenant for them.
     */
    public Long requireCurrentTenantId() {
        Long tenantId = currentTenantIdOrNull();
        if (tenantId == null) {
            throw new BadRequestException(
                    "This action requires an active Client Company context. SUPER_ADMIN users must act " +
                            "through the Client Company management endpoints instead.");
        }
        return tenantId;
    }

    /**
     * Resolves the tenant id to filter/verify resource access by:
     * - SUPER_ADMIN: the explicit filterCompanyId if supplied (may be null = unfiltered/global)
     * - tenant-scoped user: always their own tenant, ignoring anything the client supplied
     */
    public Long resolveEffectiveTenantId(Long requestedCompanyIdIfSuperAdmin) {
        if (isSuperAdmin()) {
            return requestedCompanyIdIfSuperAdmin;
        }
        return requireCurrentTenantId();
    }
}
