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
