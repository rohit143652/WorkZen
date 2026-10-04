package com.example.application.site_module.service;

import com.example.application.common.exception.BadRequestException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.security.CustomUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The single, reusable place that answers two questions for the Global Site Context feature:
 *   1. Which sites is the CURRENT user even allowed to see at all? (getAuthorizedSiteIds)
 *   2. Given whatever site filter a request asked for, what's the actually-safe filter to run
 *      the query with? (resolveRequestedSiteIds)
 *
 * No existing "which sites can this user see" concept existed anywhere in this codebase before
 * this - every current site-filterable feature (Monthly Attendance Report, Payroll site scope)
 * lets any permission-holder filter by ANY site in their own company, with no finer per-user
 * restriction. This service ADDS that finer layer without changing that default: a user is only
 * actually RESTRICTED to specific sites if their roles are exclusively site-scoped ones
 * (SITE_ADMIN/SITE_SUPERVISOR) - anyone holding a broader role (CLIENT_ADMIN, HR_ADMIN, etc.),
 * even in combination with a site-scoped one, keeps seeing every site in their company exactly
 * as they already could before this feature existed. This is a deliberately conservative choice:
 * it only ever narrows access for a role that is unambiguously site-scoped, never silently
 * narrows anyone who previously had broader access.
 */
@Service
public class SiteAccessService {

    /** Roles considered "site-scoped" - if ALL of a user's roles are in this set, their access is narrowed to their own assigned site. Any other role present means unrestricted (see class javadoc). */
    private static final Set<String> SITE_SCOPED_ROLES = Set.of("SITE_ADMIN", "SITE_SUPERVISOR");

    private final TenantContextService tenantContext;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSiteAssignmentRepository assignmentRepository;

    public SiteAccessService(TenantContextService tenantContext, EmployeeRepository employeeRepository,
                              EmployeeSiteAssignmentRepository assignmentRepository) {
        this.tenantContext = tenantContext;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * The sites the CURRENT user may access. Null means "unrestricted - every site in their
     * company" (the pre-existing behavior for everyone, and still true for SUPER_ADMIN and any
     * non-site-scoped role). A non-null set means "exactly these sites and no others".
     */
    @Transactional(readOnly = true)
    public Set<Long> getAuthorizedSiteIds(Long tenantId) {
        CustomUserPrincipal principal = tenantContext.currentPrincipalOrNull();
        if (principal == null || tenantId == null) return null; // SUPER_ADMIN / no tenant - unrestricted, unchanged from before this feature.

        Set<String> roles = principal.getRoleNames();
        boolean isPurelySiteScoped = !roles.isEmpty() && SITE_SCOPED_ROLES.containsAll(roles);
        if (!isPurelySiteScoped) return null; // Any broader role present -> unrestricted, exactly as before this feature.

        return employeeRepository.findByUserId(principal.getId())
                .flatMap(employee -> assignmentRepository
                        .findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(employee.getId(), tenantId, "ACTIVE"))
                .map(assignment -> (Set<Long>) new LinkedHashSet<>(Set.of(assignment.getSiteId())))
                // A site-scoped role with no current site assignment sees nothing rather than
                // everything - failing open here would silently undo the whole restriction.
                .orElse(Set.of());
    }

    /**
     * Given whatever siteIds a request asked for (null/empty = "give me everything I'm allowed to
     * see"), returns the actual, safe list to filter a query with. Throws if the request named a
     * site the user isn't authorized for - a client can never widen their own access by simply
     * passing a different ID (spec section 5/6: "never trust site IDs from the frontend").
     */
    @Transactional(readOnly = true)
    public List<Long> resolveRequestedSiteIds(Long tenantId, List<Long> requestedSiteIds) {
        Set<Long> authorized = getAuthorizedSiteIds(tenantId);

        if (requestedSiteIds == null || requestedSiteIds.isEmpty()) {
            // "All sites" request - resolved to the caller's own authorized set if they're
            // restricted, or left empty (meaning "no filter, every site in the company") if not.
            return authorized == null ? List.of() : List.copyOf(authorized);
        }

        if (authorized != null) {
            for (Long requested : requestedSiteIds) {
                if (!authorized.contains(requested)) {
                    throw new AccessDeniedException("You are not authorized to access site #" + requested);
                }
            }
        }
        return requestedSiteIds;
    }

    /** Same as resolveRequestedSiteIds(), but returns a 400 instead of a 403 - used by write-side flows (e.g. creating a payroll run with a chosen scope) where an invalid site selection is a form validation error more than an authorization breach, matching how those flows already report other validation problems. */
    @Transactional(readOnly = true)
    public List<Long> resolveRequestedSiteIdsOrBadRequest(Long tenantId, List<Long> requestedSiteIds) {
        try {
            return resolveRequestedSiteIds(tenantId, requestedSiteIds);
        } catch (AccessDeniedException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
