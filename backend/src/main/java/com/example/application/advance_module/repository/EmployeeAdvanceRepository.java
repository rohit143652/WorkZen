package com.example.application.advance_module.repository;

import com.example.application.advance_module.entity.EmployeeAdvance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeAdvanceRepository extends JpaRepository<EmployeeAdvance, Long> {
    Optional<EmployeeAdvance> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    List<EmployeeAdvance> findAllByClientCompanyIdAndEmployeeIdOrderByAdvanceDateDesc(Long clientCompanyId, Long employeeId);

    /** ACTIVE advances only - what recovery needs to consider each month. */
    List<EmployeeAdvance> findAllByClientCompanyIdAndEmployeeIdAndStatusOrderByAdvanceDateAsc(
            Long clientCompanyId, Long employeeId, String status);

    /** Bulk fetch for the Monthly Report - one query for every employee's active advances instead of one query per employee. */
    List<EmployeeAdvance> findAllByClientCompanyIdAndStatusOrderByEmployeeIdAscAdvanceDateAsc(Long clientCompanyId, String status);

    /** Every advance for the tenant, across all employees, newest first - for the Advance Dashboard. */
    List<EmployeeAdvance> findAllByClientCompanyIdOrderByAdvanceDateDesc(Long clientCompanyId);
}
