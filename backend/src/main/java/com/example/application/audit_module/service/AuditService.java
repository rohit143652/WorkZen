package com.example.application.audit_module.service;

import com.example.application.audit_module.dto.AuditLogResponse;
import com.example.application.audit_module.entity.AuditLog;
import com.example.application.audit_module.repository.AuditLogRepository;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.login_module.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reusable audit trail writer; any module can log an event here.
 *
 * PHASE 1 SECURITY FIX: findAll() now always scopes to the caller's own tenant unless they are a
 * genuine platform-level SUPER_ADMIN (no tenant of their own) - previously this returned every
 * company's audit trail to anyone holding AUDIT_LOG_READ, regardless of which company they
 * belonged to. See V112 migration for the full finding.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final TenantContextService tenantContextService;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, TenantContextService tenantContextService,
                         UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.tenantContextService = tenantContextService;
        this.userRepository = userRepository;
    }

    @Transactional
    public void log(Long userId, String action, String description, HttpServletRequest request) {
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setClientCompanyId(resolveClientCompanyId(userId));
        entry.setAction(action);
        entry.setDescription(description);
        if (request != null) {
            entry.setIpAddress(resolveClientIp(request));
            entry.setUserAgent(request.getHeader("User-Agent"));
        }
        auditLogRepository.save(entry);
    }

    /**
     * Two fallbacks, in order: (1) the current request's own tenant context - correct for the
     * vast majority of calls, an authenticated tenant user acting on their own company; (2) the
     * acting user's own company, looked up directly - covers calls with NO request/security
     * context at all (e.g. the public, unauthenticated employee-onboarding password-setup flow,
     * or a scheduled background job that still knows which user/employee an event concerns).
     * Returns null only when neither resolves anything - a genuinely platform-level action (e.g.
     * SUPER_ADMIN creating a new client company) or a truly system-wide event with no single
     * owning tenant at all.
     */
    private Long resolveClientCompanyId(Long userId) {
        Long fromContext = tenantContextService.currentTenantIdOrNull();
        if (fromContext != null) return fromContext;
        if (userId == null) return null;
        return userRepository.findById(userId).map(u -> u.getClientCompanyId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(Pageable pageable) {
        Long tenantId = tenantContextService.currentTenantIdOrNull();
        Page<AuditLog> page = tenantId != null
                ? auditLogRepository.findAllByClientCompanyIdOrderByCreatedAtDesc(tenantId, pageable)
                : auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(a -> new AuditLogResponse(a.getId(), a.getUserId(), a.getAction(), a.getIpAddress(),
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
