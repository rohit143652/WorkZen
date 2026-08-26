package com.example.application.payroll_module.repository;

import com.example.application.payroll_module.entity.PayrollRunEmployee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRunEmployeeRepository extends JpaRepository<PayrollRunEmployee, Long> {
    /** Idempotent-recalculation lookup - see PayrollRunService.calculate(): re-processing the same run updates this row instead of inserting a duplicate. */
    Optional<PayrollRunEmployee> findByPayrollRunIdAndEmployeeId(Long payrollRunId, Long employeeId);

    List<PayrollRunEmployee> findAllByPayrollRunIdOrderByEmployeeCodeAsc(Long payrollRunId);

    Page<PayrollRunEmployee> findAllByPayrollRunIdOrderByEmployeeCodeAsc(Long payrollRunId, Pageable pageable);

    long countByPayrollRunId(Long payrollRunId);
}
