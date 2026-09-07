package com.example.application.attendance_module.repository;

import com.example.application.attendance_module.entity.AttendanceRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceRuleConfigRepository extends JpaRepository<AttendanceRuleConfig, Long> {
    Optional<AttendanceRuleConfig> findByClientCompanyId(Long clientCompanyId);
}
