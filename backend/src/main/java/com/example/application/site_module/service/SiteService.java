package com.example.application.site_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.site_module.dto.SiteRequest;
import com.example.application.site_module.dto.SiteResponse;
import com.example.application.site_module.entity.Site;
import com.example.application.site_module.repository.SiteRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A Site belongs directly to a Client Company - there is no intermediate Sub-Client layer. */
@Service
public class SiteService {

    private static final String SITE_CODE_PREFIX = "SITE";

    private final SiteRepository siteRepository;
    private final EmployeeSiteAssignmentRepository assignmentRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public SiteService(SiteRepository siteRepository, EmployeeSiteAssignmentRepository assignmentRepository,
                        TenantContextService tenantContext, AuditService auditService) {
        this.siteRepository = siteRepository;
        this.assignmentRepository = assignmentRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<SiteResponse> findAll(Pageable pageable) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return siteRepository.findAllByClientCompanyId(tenantId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SiteResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    /** Read-only preview of the code create() would auto-assign right now - lets the Add form show/disable it upfront rather than after saving. */
    @Transactional(readOnly = true)
    public String previewNextCode() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        String lastCode = siteRepository
                .findTopByClientCompanyIdAndSiteCodeStartingWithOrderBySiteCodeDesc(tenantId, SITE_CODE_PREFIX)
                .map(Site::getSiteCode)
                .orElse(null);
        return com.example.application.common.util.CodeGeneratorService.nextCode(SITE_CODE_PREFIX, lastCode, 4);
    }

    @Transactional
    public SiteResponse create(SiteRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();

        String siteCode = request.getSiteCode();
        if (siteCode == null || siteCode.isBlank()) {
            String lastCode = siteRepository
                    .findTopByClientCompanyIdAndSiteCodeStartingWithOrderBySiteCodeDesc(tenantId, SITE_CODE_PREFIX)
                    .map(Site::getSiteCode)
                    .orElse(null);
            siteCode = com.example.application.common.util.CodeGeneratorService.nextCode(SITE_CODE_PREFIX, lastCode, 4);
        } else if (siteRepository.existsByClientCompanyIdAndSiteCode(tenantId, siteCode)) {
            throw new DuplicateResourceException("Site code already exists: " + siteCode);
        }

        Site site = new Site();
        site.setClientCompanyId(tenantId);
        applyFields(site, request);
        site.setSiteCode(siteCode);
        site.setCreatedBy(actorId);
        Site saved = siteRepository.save(site);
        auditService.log(actorId, "SITE_CREATED", "Created site " + saved.getSiteCode(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public SiteResponse update(Long id, SiteRequest request, Long actorId, HttpServletRequest httpRequest) {
        Site site = getEntity(id);

        if (!site.getSiteCode().equals(request.getSiteCode())
                && siteRepository.existsByClientCompanyIdAndSiteCode(site.getClientCompanyId(), request.getSiteCode())) {
            throw new DuplicateResourceException("Site code already exists: " + request.getSiteCode());
        }
        applyFields(site, request);
        site.setUpdatedBy(actorId);
        Site saved = siteRepository.save(site);
        auditService.log(actorId, "SITE_UPDATED", "Updated site " + saved.getSiteCode(), httpRequest);
        return toResponse(saved);
    }

    /**
     * Deactivating a site also ends every active assignment at that site, in the
     * same transaction, mirroring how deactivating an Employee or Client Company
     * cascades to disabling their logins. Employees are not deleted or touched -
     * only the assignment link is ended, exactly like a manual bulk-unassign.
     */
    @Transactional
    public SiteResponse setStatus(Long id, String status, Long actorId, HttpServletRequest httpRequest) {
        Site site = getEntity(id);
        site.setStatus(status);
        site.setUpdatedBy(actorId);
        Site saved = siteRepository.save(site);
        String action = "ACTIVE".equals(status) ? "SITE_ACTIVATED" : "SITE_DEACTIVATED";
        auditService.log(actorId, action, "Site " + saved.getSiteCode() + " status set to " + status, httpRequest);

        if ("INACTIVE".equals(status)) {
            var activeAssignments = assignmentRepository.findAllBySiteIdAndClientCompanyIdAndStatus(
                    saved.getId(), saved.getClientCompanyId(), "ACTIVE");
            for (var assignment : activeAssignments) {
                assignment.setStatus("ENDED");
                assignment.setEndDate(java.time.LocalDate.now());
                assignment.setUpdatedBy(actorId);
                assignmentRepository.save(assignment);
            }
            if (!activeAssignments.isEmpty()) {
                auditService.log(actorId, "EMPLOYEE_UNASSIGNED",
                        activeAssignments.size() + " employee(s) unassigned from " + saved.getSiteCode()
                                + " because the site was deactivated", httpRequest);
            }
        }

        return toResponse(saved);
    }

    /** Used by EmployeeAssignmentService to validate a site belongs to the current tenant. */
    @Transactional(readOnly = true)
    public Site getEntityForCurrentTenant(Long id) {
        return getEntity(id);
    }

    private Site getEntity(Long id) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return siteRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Site " + id + " does not belong to the current tenant"));
    }

    private void applyFields(Site s, SiteRequest r) {
        s.setSiteCode(r.getSiteCode());
        s.setSiteName(r.getSiteName());
        s.setDescription(r.getDescription());
        s.setAddress(r.getAddress());
        s.setCity(r.getCity());
        s.setState(r.getState());
        s.setCountry(r.getCountry());
        s.setPincode(r.getPincode());
        s.setSiteContactPerson(r.getSiteContactPerson());
        s.setSiteContactNumber(r.getSiteContactNumber());
        s.setRequiredEmployeeCount(r.getRequiredEmployeeCount());
        s.setAllowOverAllocation(r.isAllowOverAllocation());
    }

    private SiteResponse toResponse(Site s) {
        SiteResponse r = new SiteResponse();
        r.setId(s.getId());
        r.setSiteCode(s.getSiteCode());
        r.setSiteName(s.getSiteName());
        r.setDescription(s.getDescription());
        r.setAddress(s.getAddress());
        r.setCity(s.getCity());
        r.setState(s.getState());
        r.setCountry(s.getCountry());
        r.setPincode(s.getPincode());
        r.setSiteContactPerson(s.getSiteContactPerson());
        r.setSiteContactNumber(s.getSiteContactNumber());
        r.setRequiredEmployeeCount(s.getRequiredEmployeeCount());
        r.setAssignedEmployeeCount(assignmentRepository.countBySiteIdAndClientCompanyIdAndStatus(
                s.getId(), s.getClientCompanyId(), "ACTIVE"));
        r.setAllowOverAllocation(s.isAllowOverAllocation());
        r.setStatus(s.getStatus());
        r.setCreatedAt(s.getCreatedAt());
        r.setUpdatedAt(s.getUpdatedAt());
        return r;
    }
}
