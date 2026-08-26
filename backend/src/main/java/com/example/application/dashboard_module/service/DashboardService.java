package com.example.application.dashboard_module.service;

import com.example.application.client_company_module.repository.ClientCompanyRepository;
import com.example.application.dashboard_module.dto.DashboardSummaryResponse;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.site_module.repository.SiteRepository;
import com.example.application.common.tenant.TenantContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every count here is computed with an explicit tenant filter (or none, only
 * for the SUPER_ADMIN/internal-ADMIN global view) - never by loading rows into
 * Angular and filtering client-side. See TenantContextService for how the
 * tenant boundary itself is derived and enforced.
 */
@Service
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final EmployeeSiteAssignmentRepository assignmentRepository;
    private final ClientCompanyRepository clientCompanyRepository;
    private final TenantContextService tenantContextService;

    public DashboardService(EmployeeRepository employeeRepository, UserRepository userRepository,
                             SiteRepository siteRepository,
                             EmployeeSiteAssignmentRepository assignmentRepository,
                             ClientCompanyRepository clientCompanyRepository,
                             TenantContextService tenantContextService) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.assignmentRepository = assignmentRepository;
        this.clientCompanyRepository = clientCompanyRepository;
        this.tenantContextService = tenantContextService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        Long tenantId = tenantContextService.currentTenantIdOrNull();
        return tenantId != null ? tenantSummary(tenantId) : globalSummary();
    }

    private DashboardSummaryResponse tenantSummary(Long tenantId) {
        DashboardSummaryResponse r = new DashboardSummaryResponse();
        r.setGlobal(false);
        r.setTotalEmployees(employeeRepository.countByClientCompanyId(tenantId));
        r.setActiveEmployees(employeeRepository.countByClientCompanyIdAndStatus(tenantId, "ACTIVE"));
        r.setEmployeesWithLogin(employeeRepository.countByClientCompanyIdAndUserIsNotNull(tenantId));
        r.setEmployeesWithoutLogin(employeeRepository.countByClientCompanyIdAndUserIsNull(tenantId));
        r.setActiveUsers(userRepository.countByClientCompanyIdAndActiveTrueAndLockedFalse(tenantId));
        r.setLockedUsers(userRepository.countByClientCompanyIdAndLockedTrue(tenantId));
        r.setDepartments(employeeRepository.countDistinctDepartmentsForCompany(tenantId));
        r.setTotalSites(siteRepository.countByClientCompanyId(tenantId));

        long assigned = assignmentRepository.countDistinctAssignedEmployees(tenantId);
        r.setAssignedEmployees(assigned);
        r.setUnassignedEmployees(Math.max(0, r.getActiveEmployees() - assigned));
        return r;
    }

    private DashboardSummaryResponse globalSummary() {
        DashboardSummaryResponse r = new DashboardSummaryResponse();
        r.setGlobal(true);
        r.setTotalEmployees(employeeRepository.count());
        r.setActiveEmployees(employeeRepository.countByStatus("ACTIVE"));
        r.setEmployeesWithLogin(employeeRepository.countByUserIsNotNull());
        r.setEmployeesWithoutLogin(employeeRepository.countByUserIsNull());
        r.setActiveUsers(userRepository.countByActiveTrueAndLockedFalse());
        r.setLockedUsers(userRepository.countByLockedTrue());
        r.setDepartments(employeeRepository.countDistinctDepartments());
        r.setTotalSites(siteRepository.count());
        r.setTotalClientCompanies(clientCompanyRepository.count());
        r.setActiveClientCompanies(clientCompanyRepository.countByStatus("ACTIVE"));
        // Global assigned/unassigned intentionally left at 0: "assigned" is only meaningful
        // per-tenant (an assignment always belongs to exactly one client company's sites),
        // so a true global rollup would need a cross-tenant aggregate query that isn't a
        // priority for the SUPER_ADMIN overview card set requested in the spec.
        return r;
    }
}
