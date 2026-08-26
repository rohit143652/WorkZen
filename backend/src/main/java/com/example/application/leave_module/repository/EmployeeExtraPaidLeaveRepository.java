package com.example.application.leave_module.repository;

import com.example.application.leave_module.entity.EmployeeExtraPaidLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeExtraPaidLeaveRepository extends JpaRepository<EmployeeExtraPaidLeave, Long> {
    Optional<EmployeeExtraPaidLeave> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    /** Full grant history for one employee (including cancelled), most recent first. */
    List<EmployeeExtraPaidLeave> findAllByClientCompanyIdAndEmployeeIdOrderByStartDateDesc(Long clientCompanyId, Long employeeId);

    /** ACTIVE grants starting within a given month - these are what contribute to that month's extraLeave (see EmployeePaidLeaveService.resolveMonth). */
    List<EmployeeExtraPaidLeave> findAllByClientCompanyIdAndEmployeeIdAndStatusAndStartDateBetween(
            Long clientCompanyId, Long employeeId, String status, LocalDate monthStart, LocalDate monthEnd);
}
