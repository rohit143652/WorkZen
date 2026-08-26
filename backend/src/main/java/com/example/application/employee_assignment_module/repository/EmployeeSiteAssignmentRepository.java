package com.example.application.employee_assignment_module.repository;

import com.example.application.employee_assignment_module.entity.EmployeeSiteAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeSiteAssignmentRepository extends JpaRepository<EmployeeSiteAssignment, Long> {

    Optional<EmployeeSiteAssignment> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    Page<EmployeeSiteAssignment> findAllByClientCompanyId(Long clientCompanyId, Pageable pageable);

    List<EmployeeSiteAssignment> findAllByEmployeeIdAndClientCompanyIdOrderByStartDateDesc(Long employeeId, Long clientCompanyId);

    List<EmployeeSiteAssignment> findAllBySiteIdAndClientCompanyIdAndStatus(Long siteId, Long clientCompanyId, String status);

    Optional<EmployeeSiteAssignment> findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(
            Long employeeId, Long clientCompanyId, String status);

    long countBySiteIdAndClientCompanyIdAndStatus(Long siteId, Long clientCompanyId, String status);

    List<EmployeeSiteAssignment> findAllByClientCompanyIdAndStatus(Long clientCompanyId, String status);

    /** Active assignment count per site, for the manpower allocation dashboard - one query for all sites. */
    @org.springframework.data.jpa.repository.Query(
            "select a.siteId, count(a) from EmployeeSiteAssignment a " +
            "where a.clientCompanyId = :clientCompanyId and a.status = 'ACTIVE' group by a.siteId")
    List<Object[]> countActiveAssignmentsGroupedBySite(Long clientCompanyId);

    @org.springframework.data.jpa.repository.Query(
            "select count(distinct a.employeeId) from EmployeeSiteAssignment a " +
            "where a.clientCompanyId = :clientCompanyId and a.status = 'ACTIVE'")
    long countDistinctAssignedEmployees(Long clientCompanyId);
}
