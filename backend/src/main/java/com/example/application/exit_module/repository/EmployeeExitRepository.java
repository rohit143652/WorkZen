package com.example.application.exit_module.repository;

import com.example.application.exit_module.entity.EmployeeExit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeExitRepository extends JpaRepository<EmployeeExit, Long> {
    List<EmployeeExit> findAllByClientCompanyIdOrderByCreatedAtDesc(Long clientCompanyId);

    Optional<EmployeeExit> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    /** Used to block a second resignation being recorded while one is already in progress (not yet settled) for the same employee. */
    Optional<EmployeeExit> findFirstByClientCompanyIdAndEmployeeIdAndStatus(Long clientCompanyId, Long employeeId, String status);
}
