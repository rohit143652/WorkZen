package com.example.application.attendance_module.repository;

import com.example.application.attendance_module.entity.AttendanceCorrectionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceCorrectionRequestRepository extends JpaRepository<AttendanceCorrectionRequest, Long> {

    Optional<AttendanceCorrectionRequest> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    Page<AttendanceCorrectionRequest> findAllByClientCompanyIdAndEmployeeIdOrderByCreatedAtDesc(
            Long clientCompanyId, Long employeeId, Pageable pageable);

    Page<AttendanceCorrectionRequest> findAllByClientCompanyIdAndStatusOrderByCreatedAtDesc(
            Long clientCompanyId, String status, Pageable pageable);

    Page<AttendanceCorrectionRequest> findAllByClientCompanyIdOrderByCreatedAtDesc(Long clientCompanyId, Pageable pageable);
}
