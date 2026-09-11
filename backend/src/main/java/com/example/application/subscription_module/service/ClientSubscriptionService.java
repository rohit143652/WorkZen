package com.example.application.subscription_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.subscription_module.dto.ClientSubscriptionRequest;
import com.example.application.subscription_module.dto.ClientSubscriptionResponse;
import com.example.application.subscription_module.dto.SubscriptionHistoryResponse;
import com.example.application.subscription_module.dto.SuperAdminDashboardResponse;
import com.example.application.subscription_module.entity.ClientSubscription;
import com.example.application.subscription_module.entity.PlanFeature;
import com.example.application.subscription_module.entity.SubscriptionHistory;
import com.example.application.subscription_module.entity.SubscriptionPlan;
import com.example.application.subscription_module.repository.ClientSubscriptionRepository;
import com.example.application.subscription_module.repository.PlanFeatureRepository;
import com.example.application.subscription_module.repository.SubscriptionHistoryRepository;
import com.example.application.subscription_module.repository.SubscriptionPlanRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * The single, central place that:
 *   1. Assigns/changes a client company's subscription (plan, billing cycle, dates, status,
 *      overrides) - always updating the ONE current row for that tenant and appending a
 *      SubscriptionHistory entry, never creating a second "current" row.
 *   2. Enforces the active-employee limit - every caller that can create or activate an
 *      employee (EmployeeService) goes through validateCanHaveAnotherActiveEmployee() here,
 *      so the counting rule (ACTIVE only, never INACTIVE) lives in exactly one place.
 *
 * Statuses recognized: TRIAL, ACTIVE, EXPIRING_SOON, EXPIRED, GRACE_PERIOD, SUSPENDED, CANCELLED
 * - see SubscriptionAccessPolicy for what each allows a tenant to do.
 */
@Service
public class ClientSubscriptionService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "TRIAL", "ACTIVE", "EXPIRING_SOON", "EXPIRED", "GRACE_PERIOD", "SUSPENDED", "CANCELLED");
    private static final Set<String> VALID_BILLING_CYCLES = Set.of("MONTHLY", "YEARLY", "CUSTOM");

    private final ClientSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PlanFeatureRepository planFeatureRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final com.example.application.client_company_module.repository.ClientCompanyRepository clientCompanyRepository;

    public ClientSubscriptionService(ClientSubscriptionRepository subscriptionRepository, SubscriptionPlanRepository planRepository,
                                      PlanFeatureRepository planFeatureRepository, SubscriptionHistoryRepository historyRepository,
                                      EmployeeRepository employeeRepository, UserRepository userRepository, AuditService auditService,
                                      com.example.application.client_company_module.repository.ClientCompanyRepository clientCompanyRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.planFeatureRepository = planFeatureRepository;
        this.historyRepository = historyRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.clientCompanyRepository = clientCompanyRepository;
    }

    @Transactional(readOnly = true)
    public ClientSubscriptionResponse getForCompany(Long clientCompanyId) {
        return toResponse(getEntity(clientCompanyId));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionHistoryResponse> getHistory(Long clientCompanyId) {
        return historyRepository.findAllByClientCompanyIdOrderByChangeDateDesc(clientCompanyId).stream()
                .map(this::toHistoryResponse).toList();
    }

    /**
     * Creates the initial subscription for a brand-new client company - called atomically from
     * ClientCompanyService.create() in the SAME transaction as the company row itself, so a
     * failure here rolls the whole client creation back rather than leaving a company with no
     * subscription at all.
     */
    @Transactional
    public ClientSubscriptionResponse createInitial(Long clientCompanyId, ClientSubscriptionRequest request, Long actorId, HttpServletRequest httpRequest) {
        if (subscriptionRepository.findByClientCompanyId(clientCompanyId).isPresent()) {
            throw new BadRequestException("This client company already has a subscription");
        }
        SubscriptionPlan plan = getPlan(request.getPlanId());
        validateRequest(request, plan);

        ClientSubscription subscription = new ClientSubscription();
        subscription.setClientCompanyId(clientCompanyId);
        applyFields(subscription, request, plan);
        subscription.setCreatedBy(actorId);
        ClientSubscription saved = subscriptionRepository.save(subscription);

        recordHistory(clientCompanyId, null, saved, request.getReason() != null ? request.getReason() : "Initial subscription", actorId);
        auditService.log(actorId, "CLIENT_SUBSCRIPTION_CREATED",
                "Assigned plan " + plan.getPlanCode() + " to client company #" + clientCompanyId, httpRequest);
        return toResponse(saved);
    }

    /**
     * Changes an EXISTING client's plan/cycle/status/overrides. Blocks a downgrade outright if
     * the new effective employee limit would be exceeded by the client's current active
     * employee count (spec: block immediate downgrade rather than silently deactivating anyone
     * or allowing an inconsistent over-limit state).
     */
    @Transactional
    public ClientSubscriptionResponse update(Long clientCompanyId, ClientSubscriptionRequest request, Long actorId, HttpServletRequest httpRequest) {
        ClientSubscription subscription = getEntity(clientCompanyId);
        SubscriptionPlan newPlan = getPlan(request.getPlanId());
        validateRequest(request, newPlan);

        // Two-step flow: switching to a DIFFERENT plan while the current subscription is still
        // ACTIVE is blocked outright - the admin must explicitly deactivate first (a separate
        // update setting status away from ACTIVE), then assign the new plan in a second call.
        // This does NOT block editing an already-ACTIVE subscription's own details (end date,
        // notes, staying on the SAME plan) - only an actual plan switch while still active.
        boolean switchingPlan = !subscription.getPlanId().equals(newPlan.getId());
        boolean stayingActive = "ACTIVE".equals(request.getStatus());
        if ("ACTIVE".equals(subscription.getStatus()) && switchingPlan && stayingActive) {
            throw new BadRequestException("This client's subscription is currently Active on a different plan. "
                    + "Deactivate it first (set status away from Active), then assign the new plan in a separate step.");
        }

        Integer newEffectiveLimit = request.getEmployeeLimitOverride() != null ? request.getEmployeeLimitOverride() : newPlan.getEmployeeLimit();
        if (newEffectiveLimit != null) {
            long activeCount = employeeRepository.countByClientCompanyIdAndStatus(clientCompanyId, "ACTIVE");
            if (activeCount > newEffectiveLimit) {
                throw new BadRequestException("Cannot change to this plan: " + activeCount + " active employees exceed the new limit of "
                        + newEffectiveLimit + ". Deactivate employees first, or choose a higher limit.");
            }
        }

        SubscriptionPlan previousPlan = getPlan(subscription.getPlanId());
        ClientSubscription before = copyOf(subscription);
        applyFields(subscription, request, newPlan);
        subscription.setUpdatedBy(actorId);
        ClientSubscription saved = subscriptionRepository.save(subscription);

        recordHistoryFromChange(clientCompanyId, before, previousPlan, saved, newPlan, request.getReason(), actorId);
        auditService.log(actorId, "CLIENT_SUBSCRIPTION_UPDATED",
                "Changed client company #" + clientCompanyId + " subscription to plan " + newPlan.getPlanCode()
                        + ", status " + saved.getStatus(), httpRequest);
        return toResponse(saved);
    }

    /**
     * The one place every caller that can create or activate an employee must check first - see
     * EmployeeService.create()/activate()/enableLogin()/reactivate(). Counts ACTIVE employees
     * only (never INACTIVE/EXITED - see Employee.status javadoc, this codebase has no separate
     * EXITED status, an exited employee is simply INACTIVE).
     */
    @Transactional(readOnly = true)
    public void validateCanHaveAnotherActiveEmployee(Long clientCompanyId) {
        ClientSubscription subscription = subscriptionRepository.findByClientCompanyId(clientCompanyId).orElse(null);
        // No subscription row at all should not be reachable in practice (every client gets one
        // atomically at creation, and V110 backfilled every pre-existing client) - but fail open
        // rather than locking a tenant out entirely if this invariant is ever violated, since an
        // employee-limit check is a commercial control, not a safety one.
        if (subscription == null) return;
        SubscriptionPlan plan = getPlan(subscription.getPlanId());
        Integer limit = subscription.getEmployeeLimitOverride() != null ? subscription.getEmployeeLimitOverride() : plan.getEmployeeLimit();
        if (limit == null) return; // Enterprise with no configured limit at all - effectively unlimited until a number is set.
        long activeCount = employeeRepository.countByClientCompanyIdAndStatus(clientCompanyId, "ACTIVE");
        if (activeCount >= limit) {
            throw new BadRequestException("Your current subscription (" + plan.getPlanName() + ") allows up to " + limit
                    + " active employees. Upgrade your plan or deactivate an existing employee.");
        }
    }

    /**
     * Manual "deactivate now" action - distinct from the daily auto-expiry job (which sets
     * EXPIRED when the end date passes on its own). This is an explicit Super Admin decision,
     * so it's recorded as CANCELLED - the status this codebase already uses for "ended by
     * choice" rather than "ran out on its own".
     */
    @Transactional
    public ClientSubscriptionResponse deactivate(Long clientCompanyId, String reason, Long actorId, HttpServletRequest httpRequest) {
        ClientSubscription subscription = getEntity(clientCompanyId);
        String previousStatus = subscription.getStatus();
        if ("CANCELLED".equals(previousStatus)) {
            throw new BadRequestException("This subscription is already deactivated");
        }
        subscription.setStatus("CANCELLED");
        subscription.setUpdatedBy(actorId);
        ClientSubscription saved = subscriptionRepository.save(subscription);

        SubscriptionHistory h = new SubscriptionHistory();
        h.setClientCompanyId(clientCompanyId);
        h.setPreviousStatus(previousStatus);
        h.setNewStatus("CANCELLED");
        h.setReason(reason != null && !reason.isBlank() ? reason : "Manually deactivated by Super Admin");
        h.setChangedBy(actorId);
        historyRepository.save(h);

        auditService.log(actorId, "CLIENT_SUBSCRIPTION_DEACTIVATED",
                "Manually deactivated subscription for client company #" + clientCompanyId, httpRequest);
        return toResponse(saved);
    }

    private static final int EXPIRING_SOON_WINDOW_DAYS = 30;

    /**
     * Super Admin's own dashboard data - total companies and which subscriptions are ending
     * within the next 30 days. Deliberately does NOT include anything about any tenant's
     * day-to-day operations (attendance, payroll, employee counts, etc.) - that's Client Admin's
     * dashboard, not the platform owner's, matching the same "Super Admin only sees platform-
     * level things" boundary already applied to their sidebar.
     */
    @Transactional(readOnly = true)
    public SuperAdminDashboardResponse getDashboardSummary() {
        SuperAdminDashboardResponse response = new SuperAdminDashboardResponse();
        response.setTotalCompanies(clientCompanyRepository.count());
        response.setActiveCompanies(clientCompanyRepository.countByStatus("ACTIVE"));

        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(EXPIRING_SOON_WINDOW_DAYS);
        List<ClientSubscription> expiring = subscriptionRepository
                .findAllByEndDateBetweenAndStatusNotInOrderByEndDateAsc(today, windowEnd, List.of("EXPIRED", "SUSPENDED", "CANCELLED"));

        List<SuperAdminDashboardResponse.ExpiringSubscription> expiringDtos = new java.util.ArrayList<>();
        for (ClientSubscription s : expiring) {
            SuperAdminDashboardResponse.ExpiringSubscription dto = new SuperAdminDashboardResponse.ExpiringSubscription();
            dto.setClientCompanyId(s.getClientCompanyId());
            clientCompanyRepository.findById(s.getClientCompanyId())
                    .ifPresent(c -> dto.setCompanyName(c.getCompanyName()));
            dto.setPlanName(getPlan(s.getPlanId()).getPlanName());
            dto.setEndDate(s.getEndDate());
            dto.setDaysRemaining(java.time.temporal.ChronoUnit.DAYS.between(today, s.getEndDate()));
            dto.setStatus(s.getStatus());
            expiringDtos.add(dto);
        }
        response.setExpiringSoon(expiringDtos);
        return response;
    }

    private void validateRequest(ClientSubscriptionRequest request, SubscriptionPlan plan) {
        if (!VALID_STATUSES.contains(request.getStatus())) {
            throw new BadRequestException("status must be one of: " + VALID_STATUSES);
        }
        if (!VALID_BILLING_CYCLES.contains(request.getBillingCycle())) {
            throw new BadRequestException("billingCycle must be one of: " + VALID_BILLING_CYCLES);
        }
        if (plan.isCustomEmployeeLimitAllowed() && plan.getEmployeeLimit() == null && request.getEmployeeLimitOverride() == null) {
            throw new BadRequestException("This plan requires a custom employee limit - set employeeLimitOverride");
        }
        if ((plan.getMonthlyPrice() == null && request.getMonthlyPriceOverride() == null)
                || (plan.getYearlyPrice() == null && request.getYearlyPriceOverride() == null)) {
            if (plan.isCustomEmployeeLimitAllowed()) {
                // Enterprise-style plan: price overrides are expected, not required at creation time
                // (a client can be onboarded before commercial terms are finalized) - no error here.
                return;
            }
        }
    }

    private void applyFields(ClientSubscription subscription, ClientSubscriptionRequest request, SubscriptionPlan plan) {
        subscription.setPlanId(plan.getId());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setStartDate(request.getStartDate());
        subscription.setEndDate(request.getEndDate());
        subscription.setStatus(request.getStatus());
        subscription.setEmployeeLimitOverride(request.getEmployeeLimitOverride());
        subscription.setMonthlyPriceOverride(request.getMonthlyPriceOverride());
        subscription.setYearlyPriceOverride(request.getYearlyPriceOverride());
        subscription.setNotes(request.getNotes());
    }

    private void recordHistory(Long clientCompanyId, SubscriptionPlan previousPlan, ClientSubscription saved, String reason, Long actorId) {
        SubscriptionHistory h = new SubscriptionHistory();
        h.setClientCompanyId(clientCompanyId);
        h.setPreviousPlanId(previousPlan != null ? previousPlan.getId() : null);
        h.setNewPlanId(saved.getPlanId());
        h.setNewBillingCycle(saved.getBillingCycle());
        h.setNewEmployeeLimit(saved.getEmployeeLimitOverride());
        h.setNewStatus(saved.getStatus());
        h.setReason(reason);
        h.setChangedBy(actorId);
        historyRepository.save(h);
    }

    private void recordHistoryFromChange(Long clientCompanyId, ClientSubscription before, SubscriptionPlan previousPlan,
                                          ClientSubscription after, SubscriptionPlan newPlan, String reason, Long actorId) {
        SubscriptionHistory h = new SubscriptionHistory();
        h.setClientCompanyId(clientCompanyId);
        h.setPreviousPlanId(previousPlan.getId());
        h.setNewPlanId(newPlan.getId());
        h.setPreviousBillingCycle(before.getBillingCycle());
        h.setNewBillingCycle(after.getBillingCycle());
        h.setPreviousEmployeeLimit(before.getEmployeeLimitOverride() != null ? before.getEmployeeLimitOverride() : previousPlan.getEmployeeLimit());
        h.setNewEmployeeLimit(after.getEmployeeLimitOverride() != null ? after.getEmployeeLimitOverride() : newPlan.getEmployeeLimit());
        h.setPreviousStatus(before.getStatus());
        h.setNewStatus(after.getStatus());
        h.setReason(reason != null ? reason : (previousPlan.getId().equals(newPlan.getId()) ? "Subscription updated" : "Plan changed"));
        h.setChangedBy(actorId);
        historyRepository.save(h);
    }

    private ClientSubscription copyOf(ClientSubscription s) {
        ClientSubscription c = new ClientSubscription();
        c.setBillingCycle(s.getBillingCycle());
        c.setStatus(s.getStatus());
        c.setEmployeeLimitOverride(s.getEmployeeLimitOverride());
        return c;
    }

    private ClientSubscription getEntity(Long clientCompanyId) {
        return subscriptionRepository.findByClientCompanyId(clientCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for client company: " + clientCompanyId));
    }

    private SubscriptionPlan getPlan(Long planId) {
        return planRepository.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + planId));
    }

    private ClientSubscriptionResponse toResponse(ClientSubscription s) {
        SubscriptionPlan plan = getPlan(s.getPlanId());
        ClientSubscriptionResponse r = new ClientSubscriptionResponse();
        r.setId(s.getId());
        r.setClientCompanyId(s.getClientCompanyId());
        r.setPlanId(plan.getId());
        r.setPlanCode(plan.getPlanCode());
        r.setPlanName(plan.getPlanName());
        r.setBillingCycle(s.getBillingCycle());
        r.setStartDate(s.getStartDate());
        r.setEndDate(s.getEndDate());
        r.setDaysRemaining(s.getEndDate() != null ? Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), s.getEndDate())) : null);
        r.setStatus(s.getStatus());
        r.setEmployeeLimitOverride(s.getEmployeeLimitOverride());
        r.setEffectiveEmployeeLimit(s.getEmployeeLimitOverride() != null ? s.getEmployeeLimitOverride() : plan.getEmployeeLimit());
        r.setMonthlyPriceOverride(s.getMonthlyPriceOverride());
        r.setEffectiveMonthlyPrice(s.getMonthlyPriceOverride() != null ? s.getMonthlyPriceOverride() : plan.getMonthlyPrice());
        r.setYearlyPriceOverride(s.getYearlyPriceOverride());
        r.setEffectiveYearlyPrice(s.getYearlyPriceOverride() != null ? s.getYearlyPriceOverride() : plan.getYearlyPrice());
        r.setActiveEmployeeCount(employeeRepository.countByClientCompanyIdAndStatus(s.getClientCompanyId(), "ACTIVE"));
        r.setNotes(s.getNotes());
        r.setEnabledFeatures(planFeatureRepository.findAllByPlanId(plan.getId()).stream()
                .filter(PlanFeature::isEnabled).map(PlanFeature::getFeatureCode).toList());
        return r;
    }

    private SubscriptionHistoryResponse toHistoryResponse(SubscriptionHistory h) {
        SubscriptionHistoryResponse r = new SubscriptionHistoryResponse();
        r.setId(h.getId());
        r.setPreviousPlanName(h.getPreviousPlanId() != null ? planRepository.findById(h.getPreviousPlanId()).map(SubscriptionPlan::getPlanName).orElse(null) : null);
        r.setNewPlanName(h.getNewPlanId() != null ? planRepository.findById(h.getNewPlanId()).map(SubscriptionPlan::getPlanName).orElse(null) : null);
        r.setPreviousBillingCycle(h.getPreviousBillingCycle());
        r.setNewBillingCycle(h.getNewBillingCycle());
        r.setPreviousEmployeeLimit(h.getPreviousEmployeeLimit());
        r.setNewEmployeeLimit(h.getNewEmployeeLimit());
        r.setPreviousStatus(h.getPreviousStatus());
        r.setNewStatus(h.getNewStatus());
        r.setChangeDate(h.getChangeDate());
        r.setReason(h.getReason());
        if (h.getChangedBy() != null) {
            userRepository.findById(h.getChangedBy()).map(User::getUsername).ifPresent(r::setChangedByUsername);
        }
        return r;
    }
}
