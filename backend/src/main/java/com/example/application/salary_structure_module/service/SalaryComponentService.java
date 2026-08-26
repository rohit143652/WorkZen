package com.example.application.salary_structure_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.common.util.CodeGeneratorService;
import com.example.application.salary_structure_module.dto.SalaryComponentRequest;
import com.example.application.salary_structure_module.dto.SalaryComponentResponse;
import com.example.application.salary_structure_module.entity.SalaryComponent;
import com.example.application.salary_structure_module.repository.SalaryComponentRepository;
import com.example.application.salary_structure_module.repository.SalaryStructureComponentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Components are the database-driven building blocks (BASIC, HRA,
 * CONVEYANCE...) that Salary Structures are assembled from - see spec
 * sections 8-11. Architecture refactor Phase 3: only EARNING/REIMBURSEMENT
 * components may be created from here on (see
 * rejectNewDeductionOrContributionComponents) - PF/ESI/PT/Tax and other
 * monthly deductions belong to Payroll Settings + PayrollCalculationService,
 * not Salary Structure. Gated by SALARY_STRUCTURE_* permissions, same as
 * structures themselves (components are a sub-concern of structures, not a
 * separately-permissioned concept - see V40 migration).
 */
@Service
public class SalaryComponentService {

    private static final String COMPONENT_CODE_PREFIX = "SC";
    private static final Set<String> VALID_TYPES = Set.of("EARNING", "DEDUCTION", "EMPLOYER_CONTRIBUTION", "REIMBURSEMENT");
    private static final Set<String> VALID_CALC_TYPES = Set.of(
            "FIXED", "PERCENTAGE_OF_BASIC", "PERCENTAGE_OF_GROSS", "PER_DAY", "PER_HOUR", "MANUAL");

    private final SalaryComponentRepository componentRepository;
    private final SalaryStructureComponentRepository structureComponentRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public SalaryComponentService(SalaryComponentRepository componentRepository,
                                   SalaryStructureComponentRepository structureComponentRepository,
                                   TenantContextService tenantContext, AuditService auditService) {
        this.componentRepository = componentRepository;
        this.structureComponentRepository = structureComponentRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SalaryComponentResponse> findAll(boolean includeInactive) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        List<SalaryComponent> list = includeInactive
                ? componentRepository.findAllByClientCompanyIdOrderByDisplayOrderAsc(tenantId)
                : componentRepository.findAllByClientCompanyIdAndActiveOrderByDisplayOrderAsc(tenantId, true);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SalaryComponentResponse create(SalaryComponentRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        validateTypes(request.getComponentType(), request.getCalculationType());
        rejectNewDeductionOrContributionComponents(request.getComponentType());

        String code = request.getComponentCode();
        if (code == null || code.isBlank()) {
            String lastCode = componentRepository
                    .findTopByClientCompanyIdAndComponentCodeStartingWithOrderByComponentCodeDesc(tenantId, COMPONENT_CODE_PREFIX)
                    .map(SalaryComponent::getComponentCode)
                    .orElse(null);
            code = CodeGeneratorService.nextCode(COMPONENT_CODE_PREFIX, lastCode, 3);
        } else if (componentRepository.existsByClientCompanyIdAndComponentCodeIgnoreCase(tenantId, code)) {
            throw new DuplicateResourceException("Component code already exists: " + code);
        }

        SalaryComponent component = new SalaryComponent();
        component.setClientCompanyId(tenantId);
        component.setComponentCode(code);
        applyFields(component, request);
        component.setCreatedBy(actorId);
        SalaryComponent saved = componentRepository.save(component);
        auditService.log(actorId, "SALARY_STRUCTURE_UPDATED", "Added salary component " + saved.getComponentCode(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public SalaryComponentResponse update(Long id, SalaryComponentRequest request, Long actorId, HttpServletRequest httpRequest) {
        SalaryComponent component = getEntity(id);
        validateTypes(request.getComponentType(), request.getCalculationType());
        applyFields(component, request);
        SalaryComponent saved = componentRepository.save(component);
        auditService.log(actorId, "SALARY_STRUCTURE_UPDATED", "Updated salary component " + saved.getComponentCode(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public SalaryComponentResponse setStatus(Long id, boolean active, Long actorId, HttpServletRequest httpRequest) {
        SalaryComponent component = getEntity(id);
        component.setActive(active);
        SalaryComponent saved = componentRepository.save(component);
        auditService.log(actorId, "SALARY_STRUCTURE_UPDATED",
                "Salary component " + saved.getComponentCode() + " " + (active ? "activated" : "deactivated"), httpRequest);
        return toResponse(saved);
    }

    /** Used by SalaryStructureService when resolving a structure's component line items. */
    @Transactional(readOnly = true)
    public SalaryComponent getEntityForCurrentTenant(Long id) {
        return getEntity(id);
    }

    private void validateTypes(String componentType, String calculationType) {
        if (!VALID_TYPES.contains(componentType)) {
            throw new BadRequestException("Component type must be one of: " + String.join(", ", VALID_TYPES));
        }
        if (!VALID_CALC_TYPES.contains(calculationType)) {
            throw new BadRequestException("Calculation type must be one of: " + String.join(", ", VALID_CALC_TYPES));
        }
    }

    /**
     * Architecture refactor Phase 3: Salary Structure must represent Gross
     * Earnings only - PF/ESI/PT/Tax belong to Payroll Settings + the
     * employee's own pfApplicable/esiApplicable/ptApplicable flags, and
     * Advance Recovery/Other Deductions belong to the Advance module and
     * PayrollCalculationService, never to a Salary Structure component.
     * This only blocks CREATING a new DEDUCTION/EMPLOYER_CONTRIBUTION
     * component from here on - existing ones (e.g. the sample PF/ESI/PT
     * seeded for SS0001) are left untouched and still editable, since they
     * are historical data, not something to delete or block updates to.
     */
    private void rejectNewDeductionOrContributionComponents(String componentType) {
        if ("DEDUCTION".equals(componentType) || "EMPLOYER_CONTRIBUTION".equals(componentType)) {
            throw new BadRequestException(
                    "New Deduction/Employer Contribution components (PF, ESI, PT, Tax, etc.) can no longer be created here - "
                            + "these are configured in Payroll Settings and per-employee applicability instead, so every payroll "
                            + "calculation uses exactly one source of truth. Only EARNING and REIMBURSEMENT components may be created.");
        }
    }

    private void applyFields(SalaryComponent c, SalaryComponentRequest r) {
        c.setComponentName(r.getComponentName());
        c.setComponentType(r.getComponentType());
        c.setCalculationType(r.getCalculationType());
        c.setValue(r.getValue());
        c.setPercentage(r.getPercentage());
        c.setTaxable(r.isTaxable());
        c.setDisplayOrder(r.getDisplayOrder());
    }

    private SalaryComponent getEntity(Long id) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return componentRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Salary component " + id + " does not belong to the current tenant"));
    }

    private SalaryComponentResponse toResponse(SalaryComponent c) {
        long usageCount = structureComponentRepository.existsBySalaryComponentId(c.getId()) ? 1 : 0; // presence check, not a real count - fine for "in use?" UI purposes
        return new SalaryComponentResponse(c.getId(), c.getComponentCode(), c.getComponentName(), c.getComponentType(),
                c.getCalculationType(), c.getValue(), c.getPercentage(), c.isTaxable(), c.isActive(), c.getDisplayOrder(), usageCount);
    }
}
