package com.example.application.leave_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.leave_module.entity.EmployeePaidLeaveBalance;
import com.example.application.leave_module.entity.PaidLeaveConfiguration;
import com.example.application.leave_module.repository.EmployeeExtraPaidLeaveRepository;
import com.example.application.leave_module.repository.EmployeePaidLeaveBalanceRepository;
import com.example.application.login_module.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Architecture refactor Phase 9 - covers spec section 4 / section 44 CASE 4
 * exactly: turning carry-forward ON this month must NOT resurrect a
 * previous month's already-expired leave. Whether a month's surplus
 * carries forward is decided by THAT month's own policy (the source), not
 * by the destination month's policy - the original architecture audit's
 * core finding for this module.
 */
@ExtendWith(MockitoExtension.class)
class EmployeePaidLeaveServiceTest {

    @Mock private EmployeePaidLeaveBalanceRepository balanceRepository;
    @Mock private EmployeeExtraPaidLeaveRepository extraLeaveRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private LeavePolicyResolver leavePolicyResolver;
    @Mock private TenantContextService tenantContext;
    @Mock private AuditService auditService;

    @InjectMocks
    private EmployeePaidLeaveService service;

    private static final Long TENANT_ID = 1L;
    private static final Long EMPLOYEE_ID = 200L;

    @BeforeEach
    void setUp() {
        lenient().when(extraLeaveRepository.findAllByClientCompanyIdAndEmployeeIdAndStatusAndStartDateBetween(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        lenient().when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private PaidLeaveConfiguration policy(boolean carryForwardOn) {
        PaidLeaveConfiguration p = new PaidLeaveConfiguration();
        p.setMonthlyPaidLeave(2);
        p.setAllowCarryForward(carryForwardOn);
        p.setMaximumCarryForward(null);
        p.setResetAnnually(false);
        return p;
    }

    private EmployeePaidLeaveBalance balanceRow(BigDecimal available) {
        EmployeePaidLeaveBalance b = new EmployeePaidLeaveBalance();
        b.setAvailableLeave(available);
        b.setManualOverride(false);
        return b;
    }

    /** Spec section 4 / CASE 4: February had carry-forward OFF, so its 2-day surplus must expire - March turning carry-forward ON does not resurrect it. */
    @Test
    void turningCarryForwardOnDoesNotResurrectPreviousExpiredLeave() {
        // February's policy was OFF (this is the SOURCE month whose surplus we're resolving for March).
        when(leavePolicyResolver.resolve(TENANT_ID, 2026, 2)).thenReturn(policy(false));
        // March's own policy is ON - but must NOT be what gates whether February's leftover carries in.
        when(leavePolicyResolver.resolve(TENANT_ID, 2026, 3)).thenReturn(policy(true));

        // Note: no stub for February's balance row here - resolveCarryForward() correctly
        // short-circuits on prevConfig.isAllowCarryForward()==false before ever looking up
        // February's balance, so February's actual leftover amount is irrelevant to this case.
        when(balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(TENANT_ID, EMPLOYEE_ID, 2026, 3))
                .thenReturn(Optional.empty());

        EmployeePaidLeaveBalance march = service.resolveMonth(TENANT_ID, EMPLOYEE_ID, 2026, 3);

        // carryForward must be ZERO - February's OFF policy governs, not March's ON policy.
        assertEquals(0, BigDecimal.ZERO.compareTo(march.getCarryForward()));
        assertEquals(0, new BigDecimal("2").compareTo(march.getAvailableLeave())); // just March's own 2-day entitlement
    }

    /** Conversely: a month whose OWN policy is ON correctly carries its surplus into the next month. */
    @Test
    void carryForwardOnPassesSurplusToNextMonth() {
        when(leavePolicyResolver.resolve(TENANT_ID, 2026, 8)).thenReturn(policy(true)); // August (source) was ON
        when(leavePolicyResolver.resolve(TENANT_ID, 2026, 9)).thenReturn(policy(true)); // September's own config (used for its entitlement/cap)

        when(balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(TENANT_ID, EMPLOYEE_ID, 2026, 8))
                .thenReturn(Optional.of(balanceRow(new BigDecimal("1")))); // August closing = 1
        when(balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(TENANT_ID, EMPLOYEE_ID, 2026, 9))
                .thenReturn(Optional.empty());

        EmployeePaidLeaveBalance september = service.resolveMonth(TENANT_ID, EMPLOYEE_ID, 2026, 9);

        assertEquals(0, new BigDecimal("1").compareTo(september.getCarryForward()));
        assertEquals(0, new BigDecimal("3").compareTo(september.getAvailableLeave())); // 2 entitlement + 1 carried forward
    }

    /** Max carry-forward cap (spec section 7 / CASE 3) is applied using the CURRENT (destination) month's own cap. */
    @Test
    void maxCarryForwardCapsTheInheritedAmount() {
        PaidLeaveConfiguration augustPolicy = policy(true);
        PaidLeaveConfiguration septemberPolicy = policy(true);
        septemberPolicy.setMaximumCarryForward(3);
        when(leavePolicyResolver.resolve(TENANT_ID, 2026, 8)).thenReturn(augustPolicy);
        when(leavePolicyResolver.resolve(TENANT_ID, 2026, 9)).thenReturn(septemberPolicy);

        when(balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(TENANT_ID, EMPLOYEE_ID, 2026, 8))
                .thenReturn(Optional.of(balanceRow(new BigDecimal("5"))));
        when(balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(TENANT_ID, EMPLOYEE_ID, 2026, 9))
                .thenReturn(Optional.empty());

        EmployeePaidLeaveBalance september = service.resolveMonth(TENANT_ID, EMPLOYEE_ID, 2026, 9);

        assertEquals(0, new BigDecimal("3").compareTo(september.getCarryForward())); // capped at 3, not the full 5
    }

    /** previewMonth() must never write - read-only path used by the attendance report. */
    @Test
    void previewMonthNeverWrites() {
        lenient().when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndClientCompanyId(EMPLOYEE_ID, TENANT_ID)).thenReturn(Optional.of(new Employee()));
        when(balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(TENANT_ID, EMPLOYEE_ID, 2026, 3))
                .thenReturn(Optional.empty());
        when(leavePolicyResolver.resolve(eq(TENANT_ID), eq(2026), anyInt())).thenReturn(policy(true));
        when(balanceRepository.findByClientCompanyIdAndEmployeeIdAndYearAndMonth(TENANT_ID, EMPLOYEE_ID, 2026, 2))
                .thenReturn(Optional.empty());

        service.previewMonth(EMPLOYEE_ID, 2026, 3);

        verify(balanceRepository, never()).save(any());
    }
}
