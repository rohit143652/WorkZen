package com.example.application.audit_module.service;

import com.example.application.audit_module.dto.AuditLogResponse;
import com.example.application.audit_module.entity.AuditLog;
import com.example.application.audit_module.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reusable audit trail writer; any module can log an event here. */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(Long userId, String action, String description, HttpServletRequest request) {
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setAction(action);
        entry.setDescription(description);
        if (request != null) {
            entry.setIpAddress(resolveClientIp(request));
            entry.setUserAgent(request.getHeader("User-Agent"));
        }
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(a -> new AuditLogResponse(a.getId(), a.getUserId(), a.getAction(), a.getIpAddress(),
                        a.getUserAgent(), a.getDescription(), a.getCreatedAt()));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
