package com.example.application.payroll_module.service;

import com.example.application.employee_module.entity.Employee;
import com.example.application.leave_module.dto.EmployeePaidLeaveBalanceResponse;
import com.example.application.leave_module.entity.EmployeePaidLeaveBalance;
import com.example.application.leave_module.service.EmployeePaidLeaveService;
import com.example.application.salary_structure_module.service.EmployeeSalaryStructureService;
import com.example.application.salary_structure_module.service.SalaryStructureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Architecture refactor Phase 4 (TEST 4/5): viewing the Monthly Attendance
 * Report must never create or modify a Paid Leave balance row - this is
 * enforced by previewEmployeeInputs() calling EmployeePaidLeaveService
 * .previewMonth() (read-only) instead of .resolveMonth()/.recordUsage()
 * (which write). resolveEmployeeInputs() - used only by
 * PayrollRunService.calculateRun(), an explicit user action - is expected
 * to keep writing, so both are verified here.
 */
@ExtendWith(MockitoExtension.class)
class PayrollInputResolverTest {

    @Mock private EmployeePaidLeaveService paidLeaveService;
    @Mock private EmployeeSalaryStructureService employeeSalaryStructureService;
    @Mock private SalaryStructureService salaryStructureService;

    @InjectMocks
    private PayrollInputResolver resolver;

    private static final Long TENANT_ID = 1L;

    private Employee employee() {
        Employee e = new Employee();
        e.setId(200L);
        e.setEmployeeCode("EMP0001");
        e.setFirstName("Test");
        e.setLastName("Employee");
        return e;
    }

    @Test
    void previewEmployeeInputsNeverWritesToLeaveBalance() {
        when(employeeSalaryStructureService.getActiveSalaryStructure(any(), any())).thenReturn(Optional.empty());
        when(paidLeaveService.previewMonth(200L, 2026, 8)).thenReturn(
                new EmployeePaidLeaveBalanceResponse(2026, 8, new BigDecimal("2"), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, new BigDecimal("2"), false));

        resolver.previewEmployeeInputs(TENANT_ID, employee(), 2026, 8, LocalDate.of(2026, 8, 31), 31,
                List.of(), new HashMap<>());

        verify(paidLeaveService, never()).resolveMonth(anyLong(), anyLong(), anyInt(), anyInt());
        verify(paidLeaveService, never()).recordUsage(anyLong(), anyLong(), anyInt(), anyInt(), any());
        verify(paidLeaveService).previewMonth(200L, 2026, 8);
    }

    @Test
    void resolveEmployeeInputsCommitsToLeaveBalance() {
        when(employeeSalaryStructureService.getActiveSalaryStructure(any(), any())).thenReturn(Optional.empty());
        EmployeePaidLeaveBalance balance = new EmployeePaidLeaveBalance();
        balance.setMonthlyAllocation(new BigDecimal("2"));
        balance.setCarryForward(BigDecimal.ZERO);
        balance.setExtraLeave(BigDecimal.ZERO);
        balance.setUsedLeave(BigDecimal.ZERO);
        balance.setAvailableLeave(new BigDecimal("2"));
        balance.setManualOverride(false);
        when(paidLeaveService.resolveMonth(TENANT_ID, 200L, 2026, 8)).thenReturn(balance);
        when(paidLeaveService.recordUsage(eq(TENANT_ID), eq(200L), eq(2026), eq(8), any())).thenReturn(balance);

        resolver.resolveEmployeeInputs(TENANT_ID, employee(), 2026, 8, LocalDate.of(2026, 8, 31), 31,
                List.of(), new HashMap<>());

        verify(paidLeaveService).resolveMonth(TENANT_ID, 200L, 2026, 8);
        verify(paidLeaveService, never()).previewMonth(anyLong(), anyInt(), anyInt());
    }
}
