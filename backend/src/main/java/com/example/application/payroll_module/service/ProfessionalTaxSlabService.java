package com.example.application.payroll_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.payroll_module.dto.ProfessionalTaxSlabRequest;
import com.example.application.payroll_module.dto.ProfessionalTaxSlabResponse;
import com.example.application.payroll_module.entity.ProfessionalTaxSlab;
import com.example.application.payroll_module.repository.ProfessionalTaxSlabRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CRUD for a company's Professional Tax slab table, plus resolveAmount() - the actual lookup
 * PayrollInputResolver calls when PayrollSettings.ptCalculationMode is SLAB. Slabs are never
 * mutated in place once effective (same convention as PayrollSettingsService/SalaryStructure):
 * editing closes out the old row (effectiveTo) and creates a new one, so a past payroll month's
 * PT stays reproducible even if the slab structure is later changed.
 */
@Service
public class ProfessionalTaxSlabService {

    private final ProfessionalTaxSlabRepository slabRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public ProfessionalTaxSlabService(ProfessionalTaxSlabRepository slabRepository, TenantContextService tenantContext,
                                       AuditService auditService) {
        this.slabRepository = slabRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProfessionalTaxSlabResponse> findAll() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return slabRepository.findAllByClientCompanyIdOrderByEffectiveFromDescMinSalaryAsc(tenantId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ProfessionalTaxSlabResponse create(ProfessionalTaxSlabRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        validate(request);

        ProfessionalTaxSlab slab = new ProfessionalTaxSlab();
        slab.setClientCompanyId(tenantId);
        applyRequest(slab, request);
        slab.setCreatedBy(actorId);
        ProfessionalTaxSlab saved = slabRepository.save(slab);

        auditService.log(actorId, "PT_SLAB_CREATED",
                "Created Professional Tax slab " + saved.getMinSalary() + "-" + (saved.getMaxSalary() == null ? "+" : saved.getMaxSalary())
                        + " = " + saved.getPtAmount(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public ProfessionalTaxSlabResponse update(Long id, ProfessionalTaxSlabRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        ProfessionalTaxSlab slab = getForTenant(id, tenantId);
        validate(request);
        applyRequest(slab, request);
        ProfessionalTaxSlab saved = slabRepository.save(slab);
        auditService.log(actorId, "PT_SLAB_UPDATED", "Updated Professional Tax slab #" + id, httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        ProfessionalTaxSlab slab = getForTenant(id, tenantId);
        slabRepository.delete(slab);
        auditService.log(actorId, "PT_SLAB_DELETED", "Deleted Professional Tax slab #" + id, httpRequest);
    }

    /**
     * The actual lookup used by payroll calculation (PayrollInputResolver, only when
     * ptCalculationMode is SLAB) - finds the ACTIVE slab, effective as of the given date, whose
     * [minSalary, maxSalary] range contains grossSalary. Returns ZERO (not an error) when no slab
     * matches at all - a company that enabled SLAB mode but never configured any slabs should not
     * have payroll calculation fail outright, the same "fail safe, not fail loud" choice already
     * made elsewhere in this engine (e.g. an employee with no salary structure).
     */
    @Transactional(readOnly = true)
    public BigDecimal resolveAmount(Long tenantId, BigDecimal grossSalary, LocalDate asOfDate) {
        if (grossSalary == null) return BigDecimal.ZERO;
        List<ProfessionalTaxSlab> candidates = slabRepository
                .findAllByClientCompanyIdAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNull(tenantId, "ACTIVE", asOfDate);
        candidates = new java.util.ArrayList<>(candidates);
        candidates.addAll(slabRepository
                .findAllByClientCompanyIdAndStatusAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        tenantId, "ACTIVE", asOfDate, asOfDate));

        return candidates.stream()
                .filter(s -> grossSalary.compareTo(s.getMinSalary()) >= 0)
                .filter(s -> s.getMaxSalary() == null || grossSalary.compareTo(s.getMaxSalary()) <= 0)
                .findFirst()
                .map(ProfessionalTaxSlab::getPtAmount)
                .orElse(BigDecimal.ZERO);
    }

    private void validate(ProfessionalTaxSlabRequest request) {
        if (request.getMinSalary() == null || request.getMinSalary().signum() < 0) {
            throw new BadRequestException("minSalary must be zero or greater");
        }
        if (request.getMaxSalary() != null && request.getMaxSalary().compareTo(request.getMinSalary()) <= 0) {
            throw new BadRequestException("maxSalary must be greater than minSalary, or left blank for an unbounded top slab");
        }
        if (request.getPtAmount() == null || request.getPtAmount().signum() < 0) {
            throw new BadRequestException("ptAmount must be zero or greater");
        }
        if (request.getEffectiveFrom() == null) {
            throw new BadRequestException("effectiveFrom is required");
        }
    }

    private void applyRequest(ProfessionalTaxSlab slab, ProfessionalTaxSlabRequest request) {
        slab.setState(request.getState());
        slab.setMinSalary(request.getMinSalary());
        slab.setMaxSalary(request.getMaxSalary());
        slab.setPtAmount(request.getPtAmount());
        slab.setEffectiveFrom(request.getEffectiveFrom());
        slab.setEffectiveTo(request.getEffectiveTo());
    }

    private ProfessionalTaxSlab getForTenant(Long id, Long tenantId) {
        return slabRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional Tax slab not found: " + id));
    }

    private ProfessionalTaxSlabResponse toResponse(ProfessionalTaxSlab s) {
        ProfessionalTaxSlabResponse r = new ProfessionalTaxSlabResponse();
        r.setId(s.getId());
        r.setState(s.getState());
        r.setMinSalary(s.getMinSalary());
        r.setMaxSalary(s.getMaxSalary());
        r.setPtAmount(s.getPtAmount());
        r.setEffectiveFrom(s.getEffectiveFrom());
        r.setEffectiveTo(s.getEffectiveTo());
        r.setStatus(s.getStatus());
        return r;
    }
}
