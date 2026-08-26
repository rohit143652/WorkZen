package com.example.application.employee_module.repository;

import com.example.application.employee_module.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    /**
     * TENANT-SCOPED LOOKUPS - always prefer these over findById()/existsById()
     * on any endpoint reachable by a tenant-scoped user (CLIENT_ADMIN,
     * CLIENT_USER). SUPER_ADMIN-only endpoints may use the plain
     * JpaRepository methods since SUPER_ADMIN has no single tenant.
     */
    Optional<Employee> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
    boolean existsByClientCompanyIdAndEmployeeCode(Long clientCompanyId, String employeeCode);
    boolean existsByEmail(String email);
    Optional<Employee> findByUserId(Long userId);

    long countByStatus(String status);
    long countByUserIsNotNull();
    long countByUserIsNull();
    long countByClientCompanyIdAndStatus(Long clientCompanyId, String status);
    long countByClientCompanyIdAndUserIsNotNull(Long clientCompanyId);
    long countByClientCompanyIdAndUserIsNull(Long clientCompanyId);
    long countByClientCompanyId(Long clientCompanyId);
    long countByClientCompanyIdAndDepartmentIgnoreCase(Long clientCompanyId, String department);
    long countByClientCompanyIdAndDesignationIgnoreCase(Long clientCompanyId, String designation);

    @Query("select count(distinct e.department) from Employee e")
    long countDistinctDepartments();

    @Query("select count(distinct e.department) from Employee e where e.clientCompanyId = :clientCompanyId")
    long countDistinctDepartmentsForCompany(Long clientCompanyId);

    /** Used by CodeGeneratorService to derive the next EMPLOYEE code, scoped per tenant. */
    Optional<Employee> findTopByClientCompanyIdAndEmployeeCodeStartingWithOrderByEmployeeCodeDesc(Long clientCompanyId, String prefix);

    /** Full (unpaged) active-employee roster for the Monthly Attendance & Payment report. */
    java.util.List<Employee> findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(Long clientCompanyId, String status);
}
