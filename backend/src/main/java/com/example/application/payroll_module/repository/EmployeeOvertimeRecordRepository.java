package com.example.application.payroll_module.repository;

import com.example.application.payroll_module.entity.EmployeeOvertimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeOvertimeRecordRepository extends JpaRepository<EmployeeOvertimeRecord, Long> {
    Optional<EmployeeOvertimeRecord> findByClientCompanyIdAndIdAndEmployeeId(Long clientCompanyId, Long id, Long employeeId);
    Optional<EmployeeOvertimeRecord> findByEmployeeIdAndOvertimeDate(Long employeeId, LocalDate overtimeDate);

    List<EmployeeOvertimeRecord> findAllByClientCompanyIdAndEmployeeIdAndOvertimeDateBetweenOrderByOvertimeDateDesc(
            Long clientCompanyId, Long employeeId, LocalDate from, LocalDate to);

    List<EmployeeOvertimeRecord> findAllByClientCompanyIdAndOvertimeDateBetween(Long clientCompanyId, LocalDate from, LocalDate to);

    void deleteByClientCompanyIdAndIdAndEmployeeId(Long clientCompanyId, Long id, Long employeeId);
}
