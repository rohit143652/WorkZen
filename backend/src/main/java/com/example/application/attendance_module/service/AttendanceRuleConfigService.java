package com.example.application.attendance_module.service;

import com.example.application.attendance_module.dto.AttendanceRuleConfigResponse;
import com.example.application.attendance_module.dto.UpdateAttendanceRuleConfigRequest;
import com.example.application.attendance_module.entity.AttendanceRuleConfig;
import com.example.application.attendance_module.repository.AttendanceRuleConfigRepository;
import com.example.application.audit_module.service.AuditService;
import com.example.application.client_company_module.feature.FeatureAccessService;
import com.example.application.client_company_module.feature.FeatureCode;
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
    private final FeatureAccessService featureAccessService;

    public AttendanceRuleConfigService(AttendanceRuleConfigRepository repository, TenantContextService tenantContext,
                                        AuditService auditService, FeatureAccessService featureAccessService) {
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
        this.featureAccessService = featureAccessService;
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
            config.setCheckInSelfieRequired(false);
            config.setCheckOutSelfieRequired(false);
            config.setWeeklyOffDays("SUNDAY");
            return repository.save(config);
        });
    }

    /**
     * AUDIT FINDING: this (and update() below) had NO feature check at all - anyone holding
     * ATTENDANCE_READ or ATTENDANCE_SELF_MARK could successfully call this even for a company
     * with Attendance Management/Employee Self Attendance completely switched off, since the
     * permission check alone doesn't know anything about the company's feature state. Deliberately
     * added HERE and not inside getOrCreateForCurrentTenant() - that lower-level method is also
     * used internally by Payroll (working-days resolution) and the actual check-in/check-out flow,
     * both of which legitimately need this config regardless of whether the "view/manage rules"
     * feature itself is on for this company.
     */
    @Transactional(readOnly = true)
    public AttendanceRuleConfigResponse getForCurrentTenant() {
        // Matches the frontend route's exact requirement (attendance.routes.ts: feature:
        // ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE']) - both, not just one, so the
        // backend never allows something the frontend route guard would have blocked.
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.ATTENDANCE_MANAGEMENT, "Attendance Management");
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.EMPLOYEE_SELF_ATTENDANCE, "Employee Self Attendance");
        return toResponse(getOrCreateForCurrentTenant());
    }

    @Transactional
    public AttendanceRuleConfigResponse update(UpdateAttendanceRuleConfigRequest request, Long actorId, HttpServletRequest httpRequest) {
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.ATTENDANCE_MANAGEMENT, "Attendance Management");
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.EMPLOYEE_SELF_ATTENDANCE, "Employee Self Attendance");
        AttendanceRuleConfig config = getOrCreateForCurrentTenant();
        config.setOfficeStartTime(request.getOfficeStartTime());
        config.setOfficeEndTime(request.getOfficeEndTime());
        config.setRequiredWorkingMinutes(request.getRequiredWorkingMinutes());
        config.setHalfDayMinMinutes(request.getHalfDayMinMinutes());
        config.setFullDayMinMinutes(request.getFullDayMinMinutes());
        config.setLateGraceMinutes(request.getLateGraceMinutes());
        config.setDefaultBreakMinutes(request.getDefaultBreakMinutes());
        config.setAllowMultipleCheckin(request.isAllowMultipleCheckin());
        config.setCheckInSelfieRequired(request.isCheckInSelfieRequired());
        config.setCheckOutSelfieRequired(request.isCheckOutSelfieRequired());
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
        response.setCheckInSelfieRequired(config.isCheckInSelfieRequired());
        response.setCheckOutSelfieRequired(config.isCheckOutSelfieRequired());
        response.setWeeklyOffDays(config.getWeeklyOffDays());
        return response;
    }
}
