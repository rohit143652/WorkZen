package com.example.application.designation_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.designation_module.dto.DesignationRequest;
import com.example.application.designation_module.dto.DesignationResponse;
import com.example.application.designation_module.entity.Designation;
import com.example.application.designation_module.repository.DesignationRepository;
import com.example.application.employee_module.repository.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Any authenticated tenant user with DESIGNATION_READ can list designations
 * (needed to populate the Employee form dropdown); only DESIGNATION_MANAGE
 * can add/rename/activate/deactivate them. CLIENT_ADMIN has both by default
 * grant - see V21 migration - so a Client Admin can add new designations
 * on the fly without SUPER_ADMIN involvement.
 *
 * Designations are purely organisational master data now - they carry no
 * payroll structure. An employee's salary comes exclusively from the
 * Salary Structure assigned to them (see salary_structure_module).
 */
@Service
public class DesignationService {

    private final DesignationRepository designationRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public DesignationService(DesignationRepository designationRepository, EmployeeRepository employeeRepository,
                              TenantContextService tenantContext, AuditService auditService) {
        this.designationRepository = designationRepository;
        this.employeeRepository = employeeRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DesignationResponse> findAll(boolean includeInactive) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        List<Designation> list = includeInactive
                ? designationRepository.findAllByClientCompanyIdOrderByNameAsc(tenantId)
                : designationRepository.findAllByClientCompanyIdAndStatusOrderByNameAsc(tenantId, "ACTIVE");
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DesignationResponse create(DesignationRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        String name = request.getName().trim();
        if (designationRepository.existsByClientCompanyIdAndNameIgnoreCase(tenantId, name)) {
            throw new DuplicateResourceException("Designation already exists: " + name);
        }
        Designation designation = new Designation();
        designation.setClientCompanyId(tenantId);
        designation.setName(name);
        designation.setCreatedBy(actorId);
        Designation saved = designationRepository.save(designation);
        auditService.log(actorId, "DESIGNATION_CREATED", "Added designation " + saved.getName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public DesignationResponse rename(Long id, DesignationRequest request, Long actorId, HttpServletRequest httpRequest) {
        Designation designation = getEntity(id);
        String name = request.getName().trim();
        if (!designation.getName().equalsIgnoreCase(name)
                && designationRepository.existsByClientCompanyIdAndNameIgnoreCase(designation.getClientCompanyId(), name)) {
            throw new DuplicateResourceException("Designation already exists: " + name);
        }
        designation.setName(name);
        Designation saved = designationRepository.save(designation);
        auditService.log(actorId, "DESIGNATION_UPDATED", "Updated designation " + saved.getName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public DesignationResponse setStatus(Long id, String status, Long actorId, HttpServletRequest httpRequest) {
        Designation designation = getEntity(id);
        designation.setStatus(status);
        Designation saved = designationRepository.save(designation);
        auditService.log(actorId, "DESIGNATION_UPDATED",
                "Designation " + saved.getName() + " status set to " + status, httpRequest);
        return toResponse(saved);
    }

    /** Used by EmployeeService to validate a submitted designation name belongs to the current tenant. */
    @Transactional(readOnly = true)
    public boolean existsForCurrentTenant(String name) {
        Long tenantId = tenantContext.currentTenantIdOrNull();
        if (tenantId == null || name == null) return true; // SUPER_ADMIN/house context: no master-list enforcement
        return designationRepository.existsByClientCompanyIdAndNameIgnoreCase(tenantId, name.trim());
    }

    private Designation getEntity(Long id) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return designationRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Designation " + id + " does not belong to the current tenant"));
    }

    private DesignationResponse toResponse(Designation d) {
        long count = employeeRepository.countByClientCompanyIdAndDesignationIgnoreCase(d.getClientCompanyId(), d.getName());
        return new DesignationResponse(d.getId(), d.getName(), d.getStatus(), count);
    }
}
