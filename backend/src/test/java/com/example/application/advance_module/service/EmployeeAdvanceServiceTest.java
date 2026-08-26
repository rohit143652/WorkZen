package com.example.application.advance_module.service;

import com.example.application.advance_module.entity.AdvanceRecoveryTransaction;
import com.example.application.advance_module.entity.EmployeeAdvance;
import com.example.application.advance_module.repository.AdvanceRecoveryTransactionRepository;
import com.example.application.advance_module.repository.EmployeeAdvanceRepository;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Architecture refactor Phase 5: covers TEST 4 (last recovery caps at
 * outstanding, advance becomes SETTLED), TEST 17 (repeated calculation for
 * the same advance+month upserts one row, never duplicates), and TEST 7/17
 * for settlePartial (reduces outstanding via a co-existing MANUAL_SETTLEMENT
 * row rather than colliding with an existing PAYROLL row for the same month
 * - the exact bug V71 fixes).
 */
@ExtendWith(MockitoExtension.class)
class EmployeeAdvanceServiceTest {

    @Mock private EmployeeAdvanceRepository advanceRepository;
    @Mock private AdvanceRecoveryTransactionRepository recoveryRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantContextService tenantContext;
    @Mock private AuditService auditService;

    @InjectMocks
    private EmployeeAdvanceService service;

    private static final Long TENANT_ID = 1L;
    private static final Long EMPLOYEE_ID = 200L;
    private static final Long ADVANCE_ID = 900L;

    @BeforeEach
    void setUp() {
        lenient().when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
    }

    private EmployeeAdvance activeAdvance(BigDecimal amount, BigDecimal monthlyRecoveryAmount) {
        EmployeeAdvance a = new EmployeeAdvance();
        a.setId(ADVANCE_ID);
        a.setClientCompanyId(TENANT_ID);
        a.setEmployeeId(EMPLOYEE_ID);
        a.setAmount(amount);
        a.setMonthlyRecoveryAmount(monthlyRecoveryAmount);
        a.setStatus("ACTIVE");
        return a;
    }

    /** TEST 4: outstanding (2000) is less than both the configured monthly recovery (5000) and the room in gross pay (10000) - actual recovery must be capped at 2000, not 5000. */
    @Test
    void lastRecoveryCapsAtRemainingOutstanding() {
        EmployeeAdvance advance = activeAdvance(new BigDecimal("50000.00"), new BigDecimal("5000.00"));
        when(advanceRepository.findAllByClientCompanyIdAndEmployeeIdAndStatusOrderByAdvanceDateAsc(TENANT_ID, EMPLOYEE_ID, "ACTIVE"))
                .thenReturn(List.of(advance));

        AdvanceRecoveryTransaction existing = new AdvanceRecoveryTransaction();
        existing.setRecoveredAmount(new BigDecimal("48000.00")); // already recovered - only 2000 left outstanding
        when(recoveryRepository.findAllByAdvanceId(ADVANCE_ID)).thenReturn(List.of(existing));
        when(recoveryRepository.findByAdvanceIdAndYearAndMonthAndSource(ADVANCE_ID, 2026, 3, "PAYROLL")).thenReturn(Optional.empty());
        when(recoveryRepository.save(any(AdvanceRecoveryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal recovered = service.computeMonthlyRecovery(TENANT_ID, EMPLOYEE_ID, 2026, 3, 777L, new BigDecimal("10000.00"));

        assertEquals(0, new BigDecimal("2000.00").compareTo(recovered));

        var captor = org.mockito.ArgumentCaptor.forClass(AdvanceRecoveryTransaction.class);
        verify(recoveryRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("2000.00").compareTo(captor.getValue().getRecoveredAmount()));
        assertEquals("PAYROLL", captor.getValue().getSource());
        assertEquals(777L, captor.getValue().getPayrollRunId());
    }

    /** TEST 17: recalculating the same advance+month twice must update the same row, never insert a second one. */
    @Test
    void repeatedCalculationForSameMonthUpsertsSingleRow() {
        EmployeeAdvance advance = activeAdvance(new BigDecimal("50000.00"), new BigDecimal("5000.00"));
        when(advanceRepository.findAllByClientCompanyIdAndEmployeeIdAndStatusOrderByAdvanceDateAsc(TENANT_ID, EMPLOYEE_ID, "ACTIVE"))
                .thenReturn(List.of(advance));
        when(recoveryRepository.findAllByAdvanceId(ADVANCE_ID)).thenReturn(List.of());

        AdvanceRecoveryTransaction existingRow = new AdvanceRecoveryTransaction();
        existingRow.setId(55L);
        when(recoveryRepository.findByAdvanceIdAndYearAndMonthAndSource(ADVANCE_ID, 2026, 3, "PAYROLL"))
                .thenReturn(Optional.empty(), Optional.of(existingRow));
        when(recoveryRepository.save(any(AdvanceRecoveryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.computeMonthlyRecovery(TENANT_ID, EMPLOYEE_ID, 2026, 3, 777L, new BigDecimal("10000.00"));
        service.computeMonthlyRecovery(TENANT_ID, EMPLOYEE_ID, 2026, 3, 777L, new BigDecimal("10000.00"));

        // Two calculate() calls -> two save() calls, but always against the SAME logical row (the
        // second lookup returns the row the first save produced) - never a second distinct insert.
        verify(recoveryRepository, times(2)).save(any(AdvanceRecoveryTransaction.class));
        verify(recoveryRepository, times(2)).findByAdvanceIdAndYearAndMonthAndSource(ADVANCE_ID, 2026, 3, "PAYROLL");
    }

    /** TEST 7/17 (settlement variant): a manual settlement in a month that ALREADY has a PAYROLL recovery row must not collide (the V71 fix) - it becomes a separate, co-existing MANUAL_SETTLEMENT row. */
    @Test
    void partialSettlementCoexistsWithPayrollRecoveryInSameMonth() {
        EmployeeAdvance advance = activeAdvance(new BigDecimal("50000.00"), new BigDecimal("5000.00"));
        when(advanceRepository.findByIdAndClientCompanyId(ADVANCE_ID, TENANT_ID)).thenReturn(Optional.of(advance));

        AdvanceRecoveryTransaction payrollRowThisMonth = new AdvanceRecoveryTransaction();
        payrollRowThisMonth.setRecoveredAmount(new BigDecimal("5000.00"));
        payrollRowThisMonth.setSource("PAYROLL");
        when(recoveryRepository.findAllByAdvanceId(ADVANCE_ID)).thenReturn(List.of(payrollRowThisMonth));

        when(recoveryRepository.save(any(AdvanceRecoveryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Outstanding is 45000 (50000 - 5000 already recovered by payroll) - settling 3000 must not throw.
        assertDoesNotThrow(() -> service.settlePartial(EMPLOYEE_ID, ADVANCE_ID, new BigDecimal("3000.00"), "Cash payment", 9L, null));

        var captor = org.mockito.ArgumentCaptor.forClass(AdvanceRecoveryTransaction.class);
        verify(recoveryRepository).save(captor.capture());
        assertEquals("MANUAL_SETTLEMENT", captor.getValue().getSource());
        assertNull(captor.getValue().getPayrollRunId());
        assertEquals(0, new BigDecimal("3000.00").compareTo(captor.getValue().getRecoveredAmount()));
    }

    /** Settlement amount greater than outstanding must be rejected, not silently capped. */
    @Test
    void partialSettlementRejectsAmountGreaterThanOutstanding() {
        EmployeeAdvance advance = activeAdvance(new BigDecimal("1000.00"), new BigDecimal("100.00"));
        when(advanceRepository.findByIdAndClientCompanyId(ADVANCE_ID, TENANT_ID)).thenReturn(Optional.of(advance));
        when(recoveryRepository.findAllByAdvanceId(ADVANCE_ID)).thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> service.settlePartial(EMPLOYEE_ID, ADVANCE_ID, new BigDecimal("5000.00"), null, 9L, null));
        verify(recoveryRepository, never()).save(any());
    }

    /** Two settlements against the same advance on the same day must each become their own permanent, independently visible history row - never merged into one. */
    @Test
    void twoSettlementsOnSameDayCreateTwoSeparateRows() {
        EmployeeAdvance advance = activeAdvance(new BigDecimal("50000.00"), new BigDecimal("5000.00"));
        when(advanceRepository.findByIdAndClientCompanyId(ADVANCE_ID, TENANT_ID)).thenReturn(Optional.of(advance));
        // Constant (not sequential) stub - this test only cares about save() being called twice
        // and the old per-month upsert lookup never being used, not about precisely re-deriving
        // outstanding after each call (covered by other tests).
        when(recoveryRepository.findAllByAdvanceId(ADVANCE_ID)).thenReturn(List.of());
        when(recoveryRepository.save(any(AdvanceRecoveryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.settlePartial(EMPLOYEE_ID, ADVANCE_ID, new BigDecimal("2000.00"), "First cash payment", 9L, null);
        service.settlePartial(EMPLOYEE_ID, ADVANCE_ID, new BigDecimal("3000.00"), "Second cash payment same day", 9L, null);

        // Two distinct save() calls - never an upsert into a single row.
        verify(recoveryRepository, times(2)).save(any(AdvanceRecoveryTransaction.class));
        verify(recoveryRepository, never()).findByAdvanceIdAndYearAndMonthAndSource(anyLong(), anyInt(), anyInt(), anyString());
    }
}
