package com.example.application.leave_module.repository;

import com.example.application.leave_module.entity.EmployeePaidLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeePaidLeaveBalanceRepository extends JpaRepository<EmployeePaidLeaveBalance, Long> {
    Optional<EmployeePaidLeaveBalance> findByClientCompanyIdAndEmployeeIdAndYearAndMonth(
            Long clientCompanyId, Long employeeId, int year, int month);

    /** Full history for one employee, most recent first - backs getEmployeeLeaveHistory(). */
    List<EmployeePaidLeaveBalance> findAllByClientCompanyIdAndEmployeeIdOrderByYearDescMonthDesc(
            Long clientCompanyId, Long employeeId);
}
