package com.example.application.attendance_module.service;

import com.example.application.attendance_module.dto.AttendanceRuleConfigResponse;
import com.example.application.attendance_module.dto.UpdateAttendanceRuleConfigRequest;
import com.example.application.attendance_module.entity.AttendanceRuleConfig;
import com.example.application.attendance_module.repository.AttendanceRuleConfigRepository;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.tenant.TenantContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

/**
 * One config row per tenant. V97 seeds a default row for every EXISTING tenant at migration
 * time; getOrCreateForCurrentTenant() below covers any tenant created AFTER that migration ran,
 * by creating the same defaults lazily on first read rather than needing a hook wired into
 * client-company creation itself.
 */
@Service
public class AttendanceRuleConfigService {

    private final AttendanceRuleConfigRepository repository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public AttendanceRuleConfigService(AttendanceRuleConfigRepository repository, TenantContextService tenantContext,
                                        AuditService auditService) {
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional
    public AttendanceRuleConfig getOrCreateForCurrentTenant() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return repository.findByClientCompanyId(tenantId).orElseGet(() -> {
            AttendanceRuleConfig config = new AttendanceRuleConfig();
            config.setClientCompanyId(tenantId);
            config.setOfficeStartTime(LocalTime.of(9, 30));
            config.setOfficeEndTime(LocalTime.of(18, 30));
            config.setRequiredWorkingMinutes(480);
            config.setHalfDayMinMinutes(240);
            config.setFullDayMinMinutes(420);
            config.setLateGraceMinutes(10);
            config.setDefaultBreakMinutes(45);
            config.setAllowMultipleCheckin(false);
            config.setWeeklyOffDays("SUNDAY");
            return repository.save(config);
        });
    }

    @Transactional(readOnly = true)
    public AttendanceRuleConfigResponse getForCurrentTenant() {
        return toResponse(getOrCreateForCurrentTenant());
    }

    @Transactional
    public AttendanceRuleConfigResponse update(UpdateAttendanceRuleConfigRequest request, Long actorId, HttpServletRequest httpRequest) {
        AttendanceRuleConfig config = getOrCreateForCurrentTenant();
        config.setOfficeStartTime(request.getOfficeStartTime());
        config.setOfficeEndTime(request.getOfficeEndTime());
        config.setRequiredWorkingMinutes(request.getRequiredWorkingMinutes());
        config.setHalfDayMinMinutes(request.getHalfDayMinMinutes());
        config.setFullDayMinMinutes(request.getFullDayMinMinutes());
        config.setLateGraceMinutes(request.getLateGraceMinutes());
        config.setDefaultBreakMinutes(request.getDefaultBreakMinutes());
        config.setAllowMultipleCheckin(request.isAllowMultipleCheckin());
        if (request.getWeeklyOffDays() != null && !request.getWeeklyOffDays().isBlank()) {
            config.setWeeklyOffDays(request.getWeeklyOffDays());
        }
        AttendanceRuleConfig saved = repository.save(config);
        auditService.log(actorId, "ATTENDANCE_RULES_UPDATED", "Updated company attendance rules", httpRequest);
        return toResponse(saved);
    }

    private AttendanceRuleConfigResponse toResponse(AttendanceRuleConfig config) {
        AttendanceRuleConfigResponse response = new AttendanceRuleConfigResponse();
        response.setId(config.getId());
        response.setOfficeStartTime(config.getOfficeStartTime());
        response.setOfficeEndTime(config.getOfficeEndTime());
        response.setRequiredWorkingMinutes(config.getRequiredWorkingMinutes());
        response.setHalfDayMinMinutes(config.getHalfDayMinMinutes());
        response.setFullDayMinMinutes(config.getFullDayMinMinutes());
        response.setLateGraceMinutes(config.getLateGraceMinutes());
        response.setDefaultBreakMinutes(config.getDefaultBreakMinutes());
        response.setAllowMultipleCheckin(config.isAllowMultipleCheckin());
        response.setWeeklyOffDays(config.getWeeklyOffDays());
        return response;
    }
}
