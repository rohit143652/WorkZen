package com.example.application.payroll_module.service;

import com.example.application.advance_module.service.EmployeeAdvanceService;
import com.example.application.payroll_module.dto.PayrollCalculationInput;
import com.example.application.payroll_module.dto.PayrollCalculationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Covers the master spec's core worked examples: a fixed-payment employee
 * with every deduction off (TEST 1), PF enabled vs disabled (TEST 2/3),
 * and an advance larger than what's left of Gross never pushing Net Pay
 * negative (TEST 17).
 */
@ExtendWith(MockitoExtension.class)
class PayrollCalculationServiceTest {

    @Mock private EmployeeAdvanceService advanceService;

    @InjectMocks
    private PayrollCalculationService service;

    private static final Long TENANT_ID = 1L;
    private static final Long EMPLOYEE_ID = 100L;

    /** TEST 1: 15,000 fixed salary, PF/ESI/PT all off, no advance -> Net Pay = 15,000 exactly. */
    @Test
    void fixedSalaryWithNoDeductionsProducesUnchangedNetPay() {
        when(advanceService.computeMonthlyRecovery(eq(TENANT_ID), eq(EMPLOYEE_ID), eq(2026), eq(1), eq(500L), any())).thenReturn(BigDecimal.ZERO);
        when(advanceService.getOutstandingForEmployee(TENANT_ID, EMPLOYEE_ID)).thenReturn(BigDecimal.ZERO);

        PayrollCalculationInput input = PayrollCalculationInput.builder()
                .tenantId(TENANT_ID).employeeId(EMPLOYEE_ID).year(2026).month(1).payrollRunId(500L)
                .basicSalary(BigDecimal.ZERO).da(BigDecimal.ZERO).totalGross(new BigDecimal("15000.00"))
                .pf(false, BigDecimal.ZERO, BigDecimal.ZERO)
                .esi(false, BigDecimal.ZERO, BigDecimal.ZERO, null)
                .pt(false, BigDecimal.ZERO)
                .otherManualDeduction(BigDecimal.ZERO).allowance(BigDecimal.ZERO)
                .build();

        PayrollCalculationResult result = service.calculate(input);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getEpfEmployee()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getEsiEmployee()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getProfessionalTax()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getAdvanceRecovery()));
        assertEquals(0, new BigDecimal("15000.00").compareTo(result.getNetPayment()));
    }

    /** TEST 2: PF enabled -> employee/employer PF must actually be computed on Basic+DA. */
    @Test
    void pfEnabledEmployeeHasPfDeducted() {
        when(advanceService.computeMonthlyRecovery(anyLong(), anyLong(), anyInt(), anyInt(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(advanceService.getOutstandingForEmployee(anyLong(), anyLong())).thenReturn(BigDecimal.ZERO);

        PayrollCalculationInput input = PayrollCalculationInput.builder()
                .tenantId(TENANT_ID).employeeId(EMPLOYEE_ID).year(2026).month(1).payrollRunId(500L)
                .basicSalary(new BigDecimal("15000.00")).da(BigDecimal.ZERO).totalGross(new BigDecimal("20000.00"))
                .pf(true, new BigDecimal("12.00"), new BigDecimal("13.00"))
                .esi(false, BigDecimal.ZERO, BigDecimal.ZERO, null)
                .pt(false, BigDecimal.ZERO)
                .otherManualDeduction(BigDecimal.ZERO).allowance(BigDecimal.ZERO)
                .build();

        PayrollCalculationResult result = service.calculate(input);

        assertEquals(0, new BigDecimal("1800.00").compareTo(result.getEpfEmployee()));
        assertEquals(0, new BigDecimal("1950.00").compareTo(result.getEpfEmployer()));
    }

    /** TEST 3: the same employee with PF disabled must show PF = 0, not a partial/default rate. */
    @Test
    void pfDisabledEmployeeHasZeroPf() {
        when(advanceService.computeMonthlyRecovery(anyLong(), anyLong(), anyInt(), anyInt(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(advanceService.getOutstandingForEmployee(anyLong(), anyLong())).thenReturn(BigDecimal.ZERO);

        PayrollCalculationInput input = PayrollCalculationInput.builder()
                .tenantId(TENANT_ID).employeeId(EMPLOYEE_ID).year(2026).month(1).payrollRunId(500L)
                .basicSalary(new BigDecimal("15000.00")).da(BigDecimal.ZERO).totalGross(new BigDecimal("20000.00"))
                .pf(false, new BigDecimal("12.00"), new BigDecimal("13.00"))
                .esi(false, BigDecimal.ZERO, BigDecimal.ZERO, null)
                .pt(false, BigDecimal.ZERO)
                .otherManualDeduction(BigDecimal.ZERO).allowance(BigDecimal.ZERO)
                .build();

        PayrollCalculationResult result = service.calculate(input);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getEpfEmployee()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getEpfEmployer()));
    }

    /** TEST 17: an advance recovery larger than what's left of Gross must never push Net Pay negative - the shortfall stays outstanding. */
    @Test
    void advanceRecoveryNeverPushesNetPayNegative() {
        // Gross is only 15,000 but the "advance" the employee owes would need 20,000/month to
        // clear in one go - the engine must cap what it asks EmployeeAdvanceService to recover
        // at whatever room is actually left (15,000 here, since every other deduction is off).
        when(advanceService.computeMonthlyRecovery(eq(TENANT_ID), eq(EMPLOYEE_ID), eq(2026), eq(1), eq(500L), eq(new BigDecimal("15000.00"))))
                .thenReturn(new BigDecimal("15000.00"));
        when(advanceService.getOutstandingForEmployee(TENANT_ID, EMPLOYEE_ID)).thenReturn(new BigDecimal("5000.00"));

        PayrollCalculationInput input = PayrollCalculationInput.builder()
                .tenantId(TENANT_ID).employeeId(EMPLOYEE_ID).year(2026).month(1).payrollRunId(500L)
                .basicSalary(BigDecimal.ZERO).da(BigDecimal.ZERO).totalGross(new BigDecimal("15000.00"))
                .pf(false, BigDecimal.ZERO, BigDecimal.ZERO)
                .esi(false, BigDecimal.ZERO, BigDecimal.ZERO, null)
                .pt(false, BigDecimal.ZERO)
                .otherManualDeduction(BigDecimal.ZERO).allowance(BigDecimal.ZERO)
                .build();

        PayrollCalculationResult result = service.calculate(input);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getNetPayment()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getOutstandingAdvance()));
    }
}
