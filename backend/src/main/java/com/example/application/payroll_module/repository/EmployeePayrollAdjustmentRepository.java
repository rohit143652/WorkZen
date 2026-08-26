package com.example.application.payroll_module.repository;

import com.example.application.payroll_module.entity.EmployeePayrollAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeePayrollAdjustmentRepository extends JpaRepository<EmployeePayrollAdjustment, Long> {
    Optional<EmployeePayrollAdjustment> findByClientCompanyIdAndEmployeeIdAndYearAndMonth(
            Long clientCompanyId, Long employeeId, int year, int month);

    /** Bulk-fetch for the Payroll Register - avoids an N+1 lookup per employee. */
    List<EmployeePayrollAdjustment> findAllByClientCompanyIdAndYearAndMonth(Long clientCompanyId, int year, int month);
}
