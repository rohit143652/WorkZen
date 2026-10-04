package com.example.application.employee_module.repository;

import com.example.application.employee_module.entity.Employee;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {}

    public static Specification<Employee> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("employeeCode")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }

    public static Specification<Employee> hasStatus(String status) {
        return (root, query, cb) -> status == null || status.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Employee> hasDepartment(String department) {
        return (root, query, cb) -> department == null || department.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("department"), department);
    }

    public static Specification<Employee> loginEnabled(Boolean loginEnabled) {
        return (root, query, cb) -> loginEnabled == null
                ? cb.conjunction()
                : (loginEnabled ? cb.isNotNull(root.get("user")) : cb.isNull(root.get("user")));
    }

    /**
     * Global Site Context (empty/null = no filter, every employee in the company). Employee has
     * no direct siteId column - site membership lives in the separate EmployeeSiteAssignment
     * table, so this is a subquery: "this employee has a currently-ACTIVE assignment to one of
     * the given sites". An employee with no site assignment at all is correctly excluded once a
     * specific site filter is applied, matching how the Monthly Attendance Report already treats
     * unassigned employees (only shown under "All Sites", never under a specific one).
     */
    public static Specification<Employee> inSites(java.util.List<Long> siteIds) {
        return (root, query, cb) -> {
            if (siteIds == null || siteIds.isEmpty()) return cb.conjunction();
            var subquery = query.subquery(Long.class);
            var assignmentRoot = subquery.from(com.example.application.employee_assignment_module.entity.EmployeeSiteAssignment.class);
            subquery.select(assignmentRoot.get("employeeId"))
                    .where(cb.and(
                            assignmentRoot.get("siteId").in(siteIds),
                            cb.equal(assignmentRoot.get("status"), "ACTIVE")
                    ));
            return root.get("id").in(subquery);
        };
    }

    /**
     * Onboarding progress filter - accepts the exact set of statuses that make up one logical
     * filter option (see EmployeeService.search()'s onboardingFilter param for what each option
     * maps to). Passing null/empty is "no filter", matching every other Specification here.
     */
    public static Specification<Employee> hasOnboardingStatusIn(java.util.List<String> statuses) {
        return (root, query, cb) -> (statuses == null || statuses.isEmpty())
                ? cb.conjunction()
                : root.get("onboardingStatus").in(statuses);
    }

    /**
     * Mandatory tenant filter for CLIENT_ADMIN/CLIENT_USER-scoped queries.
     * SUPER_ADMIN callers pass null here and optionally combine with
     * hasClientCompany(filterCompanyId) instead for an explicit cross-tenant filter.
     */
    public static Specification<Employee> belongsToCompany(Long clientCompanyId) {
        return (root, query, cb) -> clientCompanyId == null
                ? cb.conjunction()
                : cb.equal(root.get("clientCompanyId"), clientCompanyId);
    }
}
