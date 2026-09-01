package com.example.application.leave_request_module.repository;

import com.example.application.leave_request_module.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findAllByClientCompanyIdOrderByCreatedAtDesc(Long clientCompanyId);

    List<LeaveRequest> findAllByClientCompanyIdAndEmployeeIdOrderByCreatedAtDesc(Long clientCompanyId, Long employeeId);

    Optional<LeaveRequest> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
}
