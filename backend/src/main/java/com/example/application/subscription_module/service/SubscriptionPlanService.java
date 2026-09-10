package com.example.application.subscription_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.client_company_module.feature.FeatureCode;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.subscription_module.dto.SubscriptionPlanRequest;
import com.example.application.subscription_module.dto.SubscriptionPlanResponse;
import com.example.application.subscription_module.entity.PlanFeature;
import com.example.application.subscription_module.entity.SubscriptionPlan;
import com.example.application.subscription_module.repository.ClientSubscriptionRepository;
import com.example.application.subscription_module.repository.PlanFeatureRepository;
import com.example.application.subscription_module.repository.SubscriptionPlanRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Super Admin plan management - Starter/Basic/Professional/Business/Enterprise and whatever else
 * gets added later. A plan's feature set (PlanFeature rows) is what FeatureAccessService reads
 * as the DEFAULT for any tenant subscribed to it, before that tenant's own CompanyFeature
 * overrides (if any) are applied - see FeatureAccessService.isEnabled() for the full priority.
 */
@Service
public class SubscriptionPlanService {

    /** Every valid feature code, flattened from the existing centralized catalog - a plan can never be assigned a code that doesn't actually exist here. */
    private static final Set<String> VALID_FEATURE_CODES = FeatureCode.CATALOG.stream()
            .flatMap(c -> c.codes().stream()).collect(Collectors.toSet());

    private final SubscriptionPlanRepository planRepository;
    private final PlanFeatureRepository planFeatureRepository;
    private final ClientSubscriptionRepository clientSubscriptionRepository;
    private final AuditService auditService;

    public SubscriptionPlanService(SubscriptionPlanRepository planRepository, PlanFeatureRepository planFeatureRepository,
                                    ClientSubscriptionRepository clientSubscriptionRepository, AuditService auditService) {
        this.planRepository = planRepository;
        this.planFeatureRepository = planFeatureRepository;
        this.clientSubscriptionRepository = clientSubscriptionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> findAll() {
        return planRepository.findAllByOrderByDisplayOrderAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> findAllActive() {
        return planRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public SubscriptionPlanResponse create(SubscriptionPlanRequest request, Long actorId, HttpServletRequest httpRequest) {
        validate(request);
        if (planRepository.existsByPlanCode(request.getPlanCode())) {
            throw new DuplicateResourceException("Plan code already exists: " + request.getPlanCode());
        }
        SubscriptionPlan plan = new SubscriptionPlan();
        applyFields(plan, request);
        plan.setCreatedBy(actorId);
        SubscriptionPlan saved = planRepository.save(plan);
        replaceFeatures(saved.getId(), request.getFeatureCodes());
        auditService.log(actorId, "SUBSCRIPTION_PLAN_CREATED", "Created subscription plan " + saved.getPlanCode(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionPlanResponse update(Long id, SubscriptionPlanRequest request, Long actorId, HttpServletRequest httpRequest) {
        validate(request);
        SubscriptionPlan plan = getEntity(id);
        if (!plan.getPlanCode().equals(request.getPlanCode()) && planRepository.existsByPlanCode(request.getPlanCode())) {
            throw new DuplicateResourceException("Plan code already exists: " + request.getPlanCode());
        }
        applyFields(plan, request);
        plan.setUpdatedBy(actorId);
        SubscriptionPlan saved = planRepository.save(plan);
        replaceFeatures(saved.getId(), request.getFeatureCodes());
        auditService.log(actorId, "SUBSCRIPTION_PLAN_UPDATED", "Updated subscription plan " + saved.getPlanCode(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionPlanResponse setActive(Long id, boolean active, Long actorId, HttpServletRequest httpRequest) {
        SubscriptionPlan plan = getEntity(id);
        plan.setActive(active);
        plan.setUpdatedBy(actorId);
        SubscriptionPlan saved = planRepository.save(plan);
        auditService.log(actorId, active ? "SUBSCRIPTION_PLAN_ACTIVATED" : "SUBSCRIPTION_PLAN_DEACTIVATED",
                (active ? "Activated" : "Deactivated") + " subscription plan " + saved.getPlanCode(), httpRequest);
        return toResponse(saved);
    }

    private void validate(SubscriptionPlanRequest request) {
        boolean customAllowed = Boolean.TRUE.equals(request.getCustomEmployeeLimitAllowed());
        if (!customAllowed && request.getEmployeeLimit() == null) {
            throw new BadRequestException("employeeLimit is required unless customEmployeeLimitAllowed is true");
        }
        if (request.getEmployeeLimit() != null && request.getEmployeeLimit() <= 0) {
            throw new BadRequestException("employeeLimit must be greater than 0");
        }
        if (!customAllowed && (request.getMonthlyPrice() == null || request.getYearlyPrice() == null)) {
            throw new BadRequestException("monthlyPrice and yearlyPrice are required unless customEmployeeLimitAllowed is true");
        }
        for (String code : request.getFeatureCodes()) {
            if (!VALID_FEATURE_CODES.contains(code)) {
                throw new BadRequestException("Unknown feature code: " + code);
            }
        }
    }

    private void applyFields(SubscriptionPlan plan, SubscriptionPlanRequest request) {
        plan.setPlanCode(request.getPlanCode());
        plan.setPlanName(request.getPlanName());
        plan.setDescription(request.getDescription());
        plan.setEmployeeLimit(request.getEmployeeLimit());
        plan.setCustomEmployeeLimitAllowed(Boolean.TRUE.equals(request.getCustomEmployeeLimitAllowed()));
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setYearlyPrice(request.getYearlyPrice());
        plan.setDisplayOrder(request.getDisplayOrder());
    }

    /** Full replace, not a diff/patch - simpler and matches how the Plan Form always submits the complete current feature selection, never a partial one. */
    private void replaceFeatures(Long planId, List<String> featureCodes) {
        planFeatureRepository.deleteAllByPlanId(planId);
        for (String code : Set.copyOf(featureCodes)) {
            PlanFeature pf = new PlanFeature();
            pf.setPlanId(planId);
            pf.setFeatureCode(code);
            pf.setEnabled(true);
            planFeatureRepository.save(pf);
        }
    }

    private SubscriptionPlan getEntity(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + id));
    }

    private SubscriptionPlanResponse toResponse(SubscriptionPlan p) {
        SubscriptionPlanResponse r = new SubscriptionPlanResponse();
        r.setId(p.getId());
        r.setPlanCode(p.getPlanCode());
        r.setPlanName(p.getPlanName());
        r.setDescription(p.getDescription());
        r.setEmployeeLimit(p.getEmployeeLimit());
        r.setCustomEmployeeLimitAllowed(p.isCustomEmployeeLimitAllowed());
        r.setMonthlyPrice(p.getMonthlyPrice());
        r.setYearlyPrice(p.getYearlyPrice());
        r.setActive(p.isActive());
        r.setDisplayOrder(p.getDisplayOrder());
        r.setFeatureCodes(planFeatureRepository.findAllByPlanId(p.getId()).stream()
                .filter(PlanFeature::isEnabled).map(PlanFeature::getFeatureCode).toList());
        r.setClientCount(clientSubscriptionRepository.countByPlanId(p.getId()));
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
