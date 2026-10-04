package com.example.application.site_module.service;

import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.entity.EmployeeSiteAssignment;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.role_module.entity.Role;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers the core Global Site Context authorization rules:
 * - A broad-role user (e.g. CLIENT_ADMIN) is unrestricted, exactly as before this feature existed.
 * - A purely site-scoped user (SITE_SUPERVISOR only) is restricted to their own assigned site.
 * - A site-scoped user with an ADDITIONAL broad role stays unrestricted (conservative default).
 * - Requesting a site outside one's authorized set is rejected, never silently narrowed or widened.
 * - "All sites" (empty request) resolves correctly for both restricted and unrestricted users.
 */
@ExtendWith(MockitoExtension.class)
class SiteAccessServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long EMPLOYEE_ID = 20L;
    private static final Long OWN_SITE_ID = 100L;
    private static final Long OTHER_SITE_ID = 200L;

    @Mock private TenantContextService tenantContext;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeSiteAssignmentRepository assignmentRepository;

    private SiteAccessService service;

    private void setUpService() {
        service = new SiteAccessService(tenantContext, employeeRepository, assignmentRepository);
    }

    private CustomUserPrincipal principalWithRoles(String... roleNames) {
        User user = new User();
        user.setId(USER_ID);
        Set<Role> roles = new java.util.LinkedHashSet<>();
        for (String name : roleNames) {
            Role role = new Role();
            role.setName(name);
            roles.add(role);
        }
        user.setRoles(roles);
        return new CustomUserPrincipal(user);
    }

    /** A broad-role user (CLIENT_ADMIN) is completely unrestricted - exactly the pre-existing behavior for everyone before this feature. */
    @Test
    void broadRoleUserIsUnrestricted() {
        setUpService();
        when(tenantContext.currentPrincipalOrNull()).thenReturn(principalWithRoles("CLIENT_ADMIN"));

        assertNull(service.getAuthorizedSiteIds(TENANT_ID));
    }

    /** A purely site-scoped user (SITE_SUPERVISOR only) is restricted to their own currently-assigned site. */
    @Test
    void siteScopedUserIsRestrictedToTheirOwnSite() {
        setUpService();
        when(tenantContext.currentPrincipalOrNull()).thenReturn(principalWithRoles("SITE_SUPERVISOR"));
        Employee employee = new Employee();
        employee.setId(EMPLOYEE_ID);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        EmployeeSiteAssignment assignment = new EmployeeSiteAssignment();
        assignment.setSiteId(OWN_SITE_ID);
        when(assignmentRepository.findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(EMPLOYEE_ID, TENANT_ID, "ACTIVE"))
                .thenReturn(Optional.of(assignment));

        Set<Long> authorized = service.getAuthorizedSiteIds(TENANT_ID);

        assertNotNull(authorized);
        assertEquals(Set.of(OWN_SITE_ID), authorized);
    }

    /** A site-scoped role COMBINED with any broader role stays unrestricted - the conservative default that never narrows someone who already had broader access. */
    @Test
    void siteScopedRolePlusABroaderRoleStaysUnrestricted() {
        setUpService();
        when(tenantContext.currentPrincipalOrNull()).thenReturn(principalWithRoles("SITE_SUPERVISOR", "HR_ADMIN"));

        assertNull(service.getAuthorizedSiteIds(TENANT_ID));
        verifyNoInteractions(employeeRepository);
    }

    /** A site-scoped user with NO current site assignment sees nothing, not everything - failing open here would silently undo the whole restriction. */
    @Test
    void siteScopedUserWithNoAssignmentSeesNoSites() {
        setUpService();
        when(tenantContext.currentPrincipalOrNull()).thenReturn(principalWithRoles("SITE_ADMIN"));
        Employee employee = new Employee();
        employee.setId(EMPLOYEE_ID);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(EMPLOYEE_ID, TENANT_ID, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertEquals(Set.of(), service.getAuthorizedSiteIds(TENANT_ID));
    }

    /** Requesting a site outside one's authorized set is rejected outright. */
    @Test
    void resolveRequestedSiteIdsRejectsAnUnauthorizedSite() {
        setUpService();
        when(tenantContext.currentPrincipalOrNull()).thenReturn(principalWithRoles("SITE_SUPERVISOR"));
        Employee employee = new Employee();
        employee.setId(EMPLOYEE_ID);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        EmployeeSiteAssignment assignment = new EmployeeSiteAssignment();
        assignment.setSiteId(OWN_SITE_ID);
        when(assignmentRepository.findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(EMPLOYEE_ID, TENANT_ID, "ACTIVE"))
                .thenReturn(Optional.of(assignment));

        assertThrows(AccessDeniedException.class,
                () -> service.resolveRequestedSiteIds(TENANT_ID, List.of(OTHER_SITE_ID)));
    }

    /** "All sites" (empty request) resolves to the caller's own authorized set for a restricted user. */
    @Test
    void emptyRequestResolvesToTheUsersOwnAuthorizedSitesWhenRestricted() {
        setUpService();
        when(tenantContext.currentPrincipalOrNull()).thenReturn(principalWithRoles("SITE_ADMIN"));
        Employee employee = new Employee();
        employee.setId(EMPLOYEE_ID);
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));
        EmployeeSiteAssignment assignment = new EmployeeSiteAssignment();
        assignment.setSiteId(OWN_SITE_ID);
        when(assignmentRepository.findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(EMPLOYEE_ID, TENANT_ID, "ACTIVE"))
                .thenReturn(Optional.of(assignment));

        List<Long> resolved = service.resolveRequestedSiteIds(TENANT_ID, List.of());

        assertEquals(List.of(OWN_SITE_ID), resolved);
    }

    /** "All sites" (empty request) resolves to an empty list (meaning "no filter at all") for an unrestricted user. */
    @Test
    void emptyRequestResolvesToNoFilterWhenUnrestricted() {
        setUpService();
        when(tenantContext.currentPrincipalOrNull()).thenReturn(principalWithRoles("CLIENT_ADMIN"));

        List<Long> resolved = service.resolveRequestedSiteIds(TENANT_ID, null);

        assertTrue(resolved.isEmpty());
    }

    /** No authenticated principal at all (e.g. SUPER_ADMIN, or a genuinely anonymous edge case) is unrestricted, not an error. */
    @Test
    void noPrincipalAtAllIsUnrestricted() {
        setUpService();
        when(tenantContext.currentPrincipalOrNull()).thenReturn(null);

        assertNull(service.getAuthorizedSiteIds(TENANT_ID));
    }
}
