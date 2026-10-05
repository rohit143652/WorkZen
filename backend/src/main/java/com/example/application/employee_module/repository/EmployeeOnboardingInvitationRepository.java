package com.example.application.employee_module.repository;

import com.example.application.employee_module.entity.EmployeeOnboardingInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface EmployeeOnboardingInvitationRepository extends JpaRepository<EmployeeOnboardingInvitation, Long> {
    /** Token hashes are only unique among PENDING/VERIFIED rows (see entity javadoc) - callers must additionally check status. */
    List<EmployeeOnboardingInvitation> findAllByTokenHash(String tokenHash);

    List<EmployeeOnboardingInvitation> findAllByEmployeeIdAndStatus(Long employeeId, String status);

    Optional<EmployeeOnboardingInvitation> findFirstByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    /**
     * Records the outcome of the background email send. A targeted UPDATE of just these two columns,
     * deliberately NOT load-modify-save: the employee may open the link within seconds of it being
     * sent (status PENDING -> VERIFIED), and re-saving a stale copy of the whole row would silently
     * overwrite that. Runs on the mail-sender thread, hence its own @Transactional.
     */
    @Modifying
    @Transactional
    @Query("update EmployeeOnboardingInvitation i set i.emailStatus = :status, i.emailSentAt = :sentAt where i.id = :id")
    int updateEmailStatus(@Param("id") Long id, @Param("status") String status, @Param("sentAt") LocalDateTime sentAt);
}
