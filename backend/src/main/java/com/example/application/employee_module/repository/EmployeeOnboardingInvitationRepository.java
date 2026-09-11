package com.example.application.employee_module.repository;

import com.example.application.employee_module.entity.EmployeeOnboardingInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeOnboardingInvitationRepository extends JpaRepository<EmployeeOnboardingInvitation, Long> {
    /** Token hashes are only unique among PENDING/VERIFIED rows (see entity javadoc) - callers must additionally check status. */
    List<EmployeeOnboardingInvitation> findAllByTokenHash(String tokenHash);

    List<EmployeeOnboardingInvitation> findAllByEmployeeIdAndStatus(Long employeeId, String status);

    Optional<EmployeeOnboardingInvitation> findFirstByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
