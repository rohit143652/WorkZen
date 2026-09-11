package com.example.application.audit_module.repository;

import com.example.application.audit_module.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Tenant-scoped view - only entries recorded for one specific company (see AuditLog.clientCompanyId javadoc). Used for every caller EXCEPT a genuine platform-level SUPER_ADMIN. */
    Page<AuditLog> findAllByClientCompanyIdOrderByCreatedAtDesc(Long clientCompanyId, Pageable pageable);
}
