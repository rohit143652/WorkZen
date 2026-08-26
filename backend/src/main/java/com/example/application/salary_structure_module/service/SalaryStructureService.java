package com.example.application.salary_structure_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.common.util.CodeGeneratorService;
import com.example.application.salary_structure_module.dto.*;
import com.example.application.salary_structure_module.entity.SalaryComponent;
import com.example.application.salary_structure_module.entity.SalaryStructure;
import com.example.application.salary_structure_module.entity.SalaryStructureComponent;
import com.example.application.salary_structure_module.repository.EmployeeSalaryStructureRepository;
import com.example.application.salary_structure_module.repository.SalaryStructureComponentRepository;
import com.example.application.salary_structure_module.repository.SalaryStructureRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns Salary Structure CRUD and the calculation engine that resolves a
 * structure's component line items into actual rupee amounts and a Gross
 * Earnings figure.
 *
 * ARCHITECTURE REFACTOR PHASE 3: Salary Structure answers "what does this
 * employee earn," never "what is their final Net Pay." The internal
 * calculate()/CalculationResult still computes totalDeductions/netSalary
 * for backward-compatible internal use and existing unit tests, but
 * SalaryStructureResponse no longer exposes either field - only
 * grossEarnings. PF/ESI/PT/Tax/Advance Recovery/Net Pay are exclusively
 * payroll_module.PayrollCalculationService's responsibility, fed by
 * payroll_module.PayrollInputResolver reading THIS service's
 * grossEarnings. Creating new DEDUCTION/EMPLOYER_CONTRIBUTION components
 * is blocked at SalaryComponentService.create() for exactly this reason -
 * existing ones (e.g. the sample PF/ESI/PT on SS0001, seeded before this
 * refactor) are left in place as historical data, simply no longer
 * authoritative for any payroll calculation.
 *
 * CALCULATION ENGINE (see spec sections 11, 27): deliberately does NOT
 * support FORMULA calculation (no unsafe dynamic expression evaluation).
 * Supported: FIXED, PERCENTAGE_OF_BASIC, PERCENTAGE_OF_GROSS, MANUAL.
 * PER_DAY/PER_HOUR are accepted as valid component calculation types (a
 * component can be defined that way) but are resolved as a flat reference
 * amount here, since real per-day/per-hour figures require attendance
 * data, which belongs to PayrollCalculationService - this service only
 * defines the STATIC structure, not a specific month's payroll.
 *
 * Resolution order (to avoid circularity):
 *   1. FIXED / MANUAL / PER_DAY / PER_HOUR components resolve to their own amount.
 *   2. The resolved amount(s) of any EARNING component(s) coded "BASIC" become "basic".
 *   3. PERCENTAGE_OF_BASIC components resolve to percentage% of "basic".
 *   4. "Gross Earnings" = sum of all EARNING components resolved so far - this is the
 *      one figure this service is authoritative for.
 *   5. PERCENTAGE_OF_GROSS components (earning or deduction) resolve against
 *      that Gross Earnings figure - not recursively against a
 *      Gross-including-themselves value.
 *   6-9. Total Deductions/Net Salary are still computed internally (see above) but are
 *      illustrative only from here on - never returned by the public API.
 */
@Service
public class SalaryStructureService {

    private static final String STRUCTURE_CODE_PREFIX = "SS";

    private final SalaryStructureRepository structureRepository;
    private final SalaryStructureComponentRepository structureComponentRepository;
    private final SalaryComponentService salaryComponentService;
    private final EmployeeSalaryStructureRepository employeeSalaryStructureRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public SalaryStructureService(SalaryStructureRepository structureRepository,
                                   SalaryStructureComponentRepository structureComponentRepository,
                                   SalaryComponentService salaryComponentService,
                                   EmployeeSalaryStructureRepository employeeSalaryStructureRepository,
                                   TenantContextService tenantContext, AuditService auditService) {
        this.structureRepository = structureRepository;
        this.structureComponentRepository = structureComponentRepository;
        this.salaryComponentService = salaryComponentService;
        this.employeeSalaryStructureRepository = employeeSalaryStructureRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<SalaryStructureResponse> findAll(Pageable pageable) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return structureRepository.findAllByClientCompanyId(tenantId, pageable).map(this::toResponse);
    }

    /** Lightweight, unpaged list of ACTIVE structures for pickers (e.g. the Employee form's "Salary Structure" dropdown) - spec section 27: never hardcode components/structures in Angular. */
    @Transactional(readOnly = true)
    public List<SalaryStructureResponse> findAllActive() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return structureRepository.findAllByClientCompanyIdAndStatusOrderByStructureNameAsc(tenantId, "ACTIVE")
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalaryStructureResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    /** Read-only preview of the code create() would auto-assign right now - lets the Add form show/disable it upfront rather than after saving. */
    @Transactional(readOnly = true)
    public String previewNextCode() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        String lastCode = structureRepository
                .findTopByClientCompanyIdAndStructureCodeStartingWithOrderByStructureCodeDesc(tenantId, STRUCTURE_CODE_PREFIX)
                .map(SalaryStructure::getStructureCode)
                .orElse(null);
        return CodeGeneratorService.nextCode(STRUCTURE_CODE_PREFIX, lastCode, 4);
    }

    @Transactional
    public SalaryStructureResponse create(SalaryStructureRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();

        String code = request.getStructureCode();
        if (code == null || code.isBlank()) {
            String lastCode = structureRepository
                    .findTopByClientCompanyIdAndStructureCodeStartingWithOrderByStructureCodeDesc(tenantId, STRUCTURE_CODE_PREFIX)
                    .map(SalaryStructure::getStructureCode)
                    .orElse(null);
            code = CodeGeneratorService.nextCode(STRUCTURE_CODE_PREFIX, lastCode, 4);
        } else if (structureRepository.existsByClientCompanyIdAndStructureCodeIgnoreCase(tenantId, code)) {
            throw new DuplicateResourceException("Structure code already exists: " + code);
        }

        validateSalaryType(request.getSalaryType(), request.getDailyRate(), request.getHourlyRate());

        SalaryStructure structure = new SalaryStructure();
        structure.setClientCompanyId(tenantId);
        structure.setStructureCode(code);
        structure.setStructureName(request.getStructureName());
        structure.setSalaryType(request.getSalaryType());
        structure.setDescription(request.getDescription());
        structure.setDailyRate(request.getDailyRate());
        structure.setHourlyRate(request.getHourlyRate());
        structure.setEffectiveFrom(request.getEffectiveFrom());
        structure.setEffectiveTo(request.getEffectiveTo());
        SalaryStructure saved = structureRepository.save(structure);

        saveComponents(saved.getId(), request.getComponents());

        auditService.log(actorId, "SALARY_STRUCTURE_CREATED", "Created salary structure " + saved.getStructureName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public SalaryStructureResponse update(Long id, SalaryStructureRequest request, Long actorId, HttpServletRequest httpRequest) {
        SalaryStructure structure = getEntity(id);
        if (request.getStructureCode() != null && !request.getStructureCode().isBlank()
                && !structure.getStructureCode().equalsIgnoreCase(request.getStructureCode())
                && structureRepository.existsByClientCompanyIdAndStructureCodeIgnoreCase(structure.getClientCompanyId(), request.getStructureCode())) {
            throw new DuplicateResourceException("Structure code already exists: " + request.getStructureCode());
        }
        validateSalaryType(request.getSalaryType(), request.getDailyRate(), request.getHourlyRate());
        structure.setStructureName(request.getStructureName());
        structure.setSalaryType(request.getSalaryType());
        structure.setDescription(request.getDescription());
        structure.setDailyRate(request.getDailyRate());
        structure.setHourlyRate(request.getHourlyRate());
        structure.setEffectiveFrom(request.getEffectiveFrom());
        structure.setEffectiveTo(request.getEffectiveTo());
        SalaryStructure saved = structureRepository.save(structure);

        structureComponentRepository.deleteAllBySalaryStructureId(saved.getId());
        saveComponents(saved.getId(), request.getComponents());

        auditService.log(actorId, "SALARY_STRUCTURE_UPDATED", "Updated salary structure " + saved.getStructureName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public SalaryStructureResponse setStatus(Long id, String status, Long actorId, HttpServletRequest httpRequest) {
        SalaryStructure structure = getEntity(id);
        structure.setStatus(status);
        SalaryStructure saved = structureRepository.save(structure);
        auditService.log(actorId, "SALARY_STRUCTURE_UPDATED",
                "Salary structure " + saved.getStructureName() + " status set to " + status, httpRequest);
        return toResponse(saved);
    }

    /** Copies a structure's name/components under a new code, for the "start from an existing structure" workflow. */
    @Transactional
    public SalaryStructureResponse duplicate(Long id, Long actorId, HttpServletRequest httpRequest) {
        SalaryStructure original = getEntity(id);
        Long tenantId = original.getClientCompanyId();

        String lastCode = structureRepository
                .findTopByClientCompanyIdAndStructureCodeStartingWithOrderByStructureCodeDesc(tenantId, STRUCTURE_CODE_PREFIX)
                .map(SalaryStructure::getStructureCode)
                .orElse(null);
        String newCode = CodeGeneratorService.nextCode(STRUCTURE_CODE_PREFIX, lastCode, 4);

        SalaryStructure copy = new SalaryStructure();
        copy.setClientCompanyId(tenantId);
        copy.setStructureCode(newCode);
        copy.setStructureName(original.getStructureName() + " (Copy)");
        copy.setSalaryType(original.getSalaryType());
        copy.setDescription(original.getDescription());
        copy.setDailyRate(original.getDailyRate());
        copy.setHourlyRate(original.getHourlyRate());
        copy.setEffectiveFrom(original.getEffectiveFrom());
        copy.setEffectiveTo(original.getEffectiveTo());
        copy.setCreatedBy(actorId);
        SalaryStructure savedCopy = structureRepository.save(copy);

        for (SalaryStructureComponent sc : structureComponentRepository.findAllBySalaryStructureIdOrderByDisplayOrderAsc(original.getId())) {
            SalaryStructureComponent newSc = new SalaryStructureComponent();
            newSc.setSalaryStructureId(savedCopy.getId());
            newSc.setSalaryComponentId(sc.getSalaryComponentId());
            newSc.setCalculationType(sc.getCalculationType());
            newSc.setAmount(sc.getAmount());
            newSc.setPercentage(sc.getPercentage());
            newSc.setActive(sc.isActive());
            newSc.setDisplayOrder(sc.getDisplayOrder());
            structureComponentRepository.save(newSc);
        }

        auditService.log(actorId, "SALARY_STRUCTURE_CREATED",
                "Duplicated salary structure " + original.getStructureName() + " as " + savedCopy.getStructureName(), httpRequest);
        return toResponse(savedCopy);
    }

    @Transactional
    public void delete(Long id, Long actorId, HttpServletRequest httpRequest) {
        SalaryStructure structure = getEntity(id);
        if (employeeSalaryStructureRepository.existsBySalaryStructureId(id)) {
            throw new BadRequestException(
                    "This structure has been assigned to an employee at least once and cannot be deleted. Deactivate it instead.");
        }
        structureComponentRepository.deleteAllBySalaryStructureId(id);
        structureRepository.delete(structure);
        auditService.log(actorId, "SALARY_STRUCTURE_UPDATED", "Deleted salary structure " + structure.getStructureName(), httpRequest);
    }

    /** Used by EmployeeSalaryStructureService to validate a structure ID belongs to the current tenant before assigning it. */
    @Transactional(readOnly = true)
    public SalaryStructure getEntityForCurrentTenant(Long id) {
        return getEntity(id);
    }

    private static final java.util.Set<String> VALID_SALARY_TYPES = java.util.Set.of("MONTHLY", "DAILY", "HOURLY", "CONTRACT");

    /** Spec sections 13-16: DAILY structures need a reference dailyRate, HOURLY need an hourlyRate. Not enforced for MONTHLY/CONTRACT, which are driven entirely by components. */
    private void validateSalaryType(String salaryType, BigDecimal dailyRate, BigDecimal hourlyRate) {
        if (salaryType == null || !VALID_SALARY_TYPES.contains(salaryType)) {
            throw new BadRequestException("Salary type must be one of MONTHLY, DAILY, HOURLY, CONTRACT");
        }
        if ("DAILY".equals(salaryType) && (dailyRate == null || dailyRate.signum() < 0)) {
            throw new BadRequestException("Daily rate is required (and must be >= 0) for a DAILY salary type");
        }
        if ("HOURLY".equals(salaryType) && (hourlyRate == null || hourlyRate.signum() < 0)) {
            throw new BadRequestException("Hourly rate is required (and must be >= 0) for an HOURLY salary type");
        }
    }

    private void saveComponents(Long structureId, List<SalaryStructureComponentRequest> requests) {
        int order = 0;
        for (SalaryStructureComponentRequest req : requests) {
            // Validates the component belongs to this tenant, 404s otherwise.
            salaryComponentService.getEntityForCurrentTenant(req.getSalaryComponentId());

            SalaryStructureComponent sc = new SalaryStructureComponent();
            sc.setSalaryStructureId(structureId);
            sc.setSalaryComponentId(req.getSalaryComponentId());
            sc.setCalculationType(req.getCalculationType());
            sc.setAmount(req.getAmount());
            sc.setPercentage(req.getPercentage());
            sc.setActive(true);
            sc.setDisplayOrder(req.getDisplayOrder() != 0 ? req.getDisplayOrder() : order);
            structureComponentRepository.save(sc);
            order++;
        }
    }

    private SalaryStructure getEntity(Long id) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return structureRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Salary structure " + id + " does not belong to the current tenant"));
    }

    private SalaryStructureResponse toResponse(SalaryStructure structure) {
        List<SalaryStructureComponent> lineItems = structureComponentRepository
                .findAllBySalaryStructureIdOrderByDisplayOrderAsc(structure.getId());

        CalculationResult result = calculate(lineItems);

        SalaryStructureResponse response = new SalaryStructureResponse();
        response.setId(structure.getId());
        response.setStructureCode(structure.getStructureCode());
        response.setStructureName(structure.getStructureName());
        response.setSalaryType(structure.getSalaryType());
        response.setDescription(structure.getDescription());
        response.setDailyRate(structure.getDailyRate());
        response.setHourlyRate(structure.getHourlyRate());
        response.setEffectiveFrom(structure.getEffectiveFrom());
        response.setEffectiveTo(structure.getEffectiveTo());
        response.setStatus(structure.getStatus());
        response.setComponents(result.componentResponses);
        // Architecture refactor Phase 3: Gross Earnings only - result.totalDeductions/netSalary
        // are still computed internally (and still checked by unit tests against this exact
        // CalculationResult), but are deliberately not exposed here anymore. PF/ESI/PT/Tax/
        // Advance Recovery/Net Pay are payroll_module.PayrollCalculationService's job alone.
        response.setGrossEarnings(result.grossEarnings);
        response.setEmployeeCount(countActiveAssignments(structure.getId()));
        response.setCreatedAt(structure.getCreatedAt());
        response.setUpdatedAt(structure.getUpdatedAt());
        return response;
    }

    private long countActiveAssignments(Long structureId) {
        // Simple existence-based signal is enough for the UI's "cannot delete" check;
        // a full count isn't currently exposed by the repository and isn't needed elsewhere.
        return employeeSalaryStructureRepository.existsBySalaryStructureId(structureId) ? 1 : 0;
    }

    /** Package-private so EmployeeSalaryStructureService can reuse the same calculation for a structure without duplicating logic. */
    CalculationResult calculateForStructure(Long structureId) {
        List<SalaryStructureComponent> lineItems = structureComponentRepository
                .findAllBySalaryStructureIdOrderByDisplayOrderAsc(structureId);
        return calculate(lineItems);
    }

    private CalculationResult calculate(List<SalaryStructureComponent> lineItems) {
        Map<Long, SalaryComponent> componentsById = new HashMap<>();
        for (SalaryStructureComponent sc : lineItems) {
            componentsById.computeIfAbsent(sc.getSalaryComponentId(), salaryComponentService::getEntityForCurrentTenant);
        }

        Map<Long, BigDecimal> resolved = new HashMap<>();

        // Step 1: FIXED / MANUAL / PER_DAY / PER_HOUR resolve to their own amount.
        for (SalaryStructureComponent sc : lineItems) {
            if (!sc.isActive()) continue;
            String calcType = sc.getCalculationType();
            if ("FIXED".equals(calcType) || "MANUAL".equals(calcType) || "PER_DAY".equals(calcType) || "PER_HOUR".equals(calcType)) {
                resolved.put(sc.getId(), sc.getAmount() != null ? sc.getAmount() : BigDecimal.ZERO);
            }
        }

        // Step 2: find "basic" = resolved EARNING component(s) coded BASIC.
        BigDecimal basic = BigDecimal.ZERO;
        for (SalaryStructureComponent sc : lineItems) {
            if (!sc.isActive()) continue;
            SalaryComponent component = componentsById.get(sc.getSalaryComponentId());
            if ("EARNING".equals(component.getComponentType()) && "BASIC".equalsIgnoreCase(component.getComponentCode())
                    && resolved.containsKey(sc.getId())) {
                basic = basic.add(resolved.get(sc.getId()));
            }
        }

        // Step 3: PERCENTAGE_OF_BASIC resolves against "basic".
        for (SalaryStructureComponent sc : lineItems) {
            if (!sc.isActive() || resolved.containsKey(sc.getId())) continue;
            if ("PERCENTAGE_OF_BASIC".equals(sc.getCalculationType())) {
                BigDecimal pct = sc.getPercentage() != null ? sc.getPercentage() : BigDecimal.ZERO;
                resolved.put(sc.getId(), basic.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        }

        // Step 4: Gross Earnings so far = sum of resolved EARNING components.
        BigDecimal grossSoFar = BigDecimal.ZERO;
        for (SalaryStructureComponent sc : lineItems) {
            if (!sc.isActive() || !resolved.containsKey(sc.getId())) continue;
            SalaryComponent component = componentsById.get(sc.getSalaryComponentId());
            if ("EARNING".equals(component.getComponentType())) {
                grossSoFar = grossSoFar.add(resolved.get(sc.getId()));
            }
        }

        // Step 5: PERCENTAGE_OF_GROSS resolves against grossSoFar (not recursively against itself).
        for (SalaryStructureComponent sc : lineItems) {
            if (!sc.isActive() || resolved.containsKey(sc.getId())) continue;
            if ("PERCENTAGE_OF_GROSS".equals(sc.getCalculationType())) {
                BigDecimal pct = sc.getPercentage() != null ? sc.getPercentage() : BigDecimal.ZERO;
                resolved.put(sc.getId(), grossSoFar.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        }

        // Anything still unresolved (shouldn't normally happen) defaults to zero rather than NPE-ing.
        for (SalaryStructureComponent sc : lineItems) {
            resolved.putIfAbsent(sc.getId(), BigDecimal.ZERO);
        }

        BigDecimal grossEarnings = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal reimbursements = BigDecimal.ZERO;
        List<SalaryStructureComponentResponse> componentResponses = new ArrayList<>();

        for (SalaryStructureComponent sc : lineItems) {
            SalaryComponent component = componentsById.get(sc.getSalaryComponentId());
            BigDecimal amount = resolved.get(sc.getId());

            if (sc.isActive()) {
                switch (component.getComponentType()) {
                    case "EARNING" -> grossEarnings = grossEarnings.add(amount);
                    case "DEDUCTION" -> totalDeductions = totalDeductions.add(amount);
                    case "REIMBURSEMENT" -> reimbursements = reimbursements.add(amount);
                    default -> { /* EMPLOYER_CONTRIBUTION: tracked on the response but excluded from employee net */ }
                }
            }

            componentResponses.add(new SalaryStructureComponentResponse(
                    sc.getId(), sc.getSalaryComponentId(), component.getComponentCode(), component.getComponentName(),
                    component.getComponentType(), sc.getCalculationType(), sc.getAmount(), sc.getPercentage(),
                    amount, sc.getDisplayOrder()));
        }

        BigDecimal netSalary = grossEarnings.subtract(totalDeductions).add(reimbursements);

        CalculationResult result = new CalculationResult();
        result.componentResponses = componentResponses;
        result.grossEarnings = grossEarnings;
        result.totalDeductions = totalDeductions;
        result.netSalary = netSalary;
        return result;
    }

    static class CalculationResult {
        List<SalaryStructureComponentResponse> componentResponses;
        BigDecimal grossEarnings;
        BigDecimal totalDeductions;
        BigDecimal netSalary;
    }
}
