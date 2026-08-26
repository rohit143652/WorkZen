package com.example.application.salary_structure_module.repository;

import com.example.application.salary_structure_module.entity.EmployeeSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeSalaryStructureRepository extends JpaRepository<EmployeeSalaryStructure, Long> {
    List<EmployeeSalaryStructure> findAllByClientCompanyIdAndEmployeeIdOrderByEffectiveFromDesc(Long clientCompanyId, Long employeeId);
    Optional<EmployeeSalaryStructure> findFirstByClientCompanyIdAndEmployeeIdAndStatusOrderByEffectiveFromDesc(
            Long clientCompanyId, Long employeeId, String status);
    boolean existsBySalaryStructureId(Long salaryStructureId);

    /**
     * Resolves whichever assignment row was in force on a given calendar date - the
     * cornerstone query the future Payroll module needs (spec section 61: "August payroll
     * must still use August salary" even after a September salary change). Not restricted to
     * status='ACTIVE' because a since-ended (status='ENDED') row is still the historically
     * correct answer for a past payrollDate; effectiveTo IS NULL covers the current open-ended row.
     */
    @Query("SELECT e FROM EmployeeSalaryStructure e WHERE e.clientCompanyId = :tenantId AND e.employeeId = :employeeId " +
            "AND e.effectiveFrom <= :onDate AND (e.effectiveTo IS NULL OR e.effectiveTo >= :onDate) " +
            "ORDER BY e.effectiveFrom DESC")
    Optional<EmployeeSalaryStructure> findEffectiveOn(@Param("tenantId") Long tenantId,
                                                       @Param("employeeId") Long employeeId,
                                                       @Param("onDate") LocalDate onDate);
}
