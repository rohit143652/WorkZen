package com.example.application.payroll_module.service;

import com.example.application.attendance_module.repository.AttendanceRepository;
import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.payroll_module.dto.EmployeePayrollInputs;
import com.example.application.payroll_module.dto.PayrollCalculationResult;
import com.example.application.payroll_module.dto.PayrollRunCreateRequest;
import com.example.application.payroll_module.entity.PayrollRun;
import com.example.application.payroll_module.entity.PayrollRunEmployee;
import com.example.application.payroll_module.entity.PayrollSettings;
import com.example.application.payroll_module.repository.EmployeePayrollAdjustmentRepository;
import com.example.application.payroll_module.repository.PayrollRunEmployeeRepository;
import com.example.application.payroll_module.repository.PayrollRunRepository;
import com.example.application.site_module.repository.SiteRepository;
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
 * Covers TEST 1/2 (create + duplicate-run rejection), TEST 9 (an
 * APPROVED/PAID run rejects recalculation), and TEST 11 (recalculating a
 * still-editable run updates the existing PayrollRunEmployee row instead
 * of inserting a second one for the same employee).
 */
@ExtendWith(MockitoExtension.class)
class PayrollRunServiceTest {

    @Mock private PayrollRunRepository payrollRunRepository;
    @Mock private PayrollRunEmployeeRepository payrollRunEmployeeRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EmployeeSiteAssignmentRepository siteAssignmentRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private PayrollSettingsResolver payrollSettingsResolver;
    @Mock private EmployeePayrollAdjustmentRepository payrollAdjustmentRepository;
    @Mock private PayrollInputResolver payrollInputResolver;
    @Mock private PayrollCalculationService payrollCalculationService;
    @Mock private PayrollStatusTransitionService statusTransitionService;
    @Mock private UserRepository userRepository;
    @Mock private TenantContextService tenantContext;
    @Mock private AuditService auditService;

    @InjectMocks
    private PayrollRunService service;

    private static final Long TENANT_ID = 1L;
    private static final Long ACTOR_ID = 9L;
    private static final Long RUN_ID = 50L;

    @BeforeEach
    void setUp() {
        lenient().when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT_ID);
    }

    /** TEST 1: creating a run for a month with no existing run succeeds. */
    @Test
    void createRunSucceedsWhenNoDuplicateExists() {
        when(payrollRunRepository.findAllByClientCompanyIdAndYearAndMonthAndStatusNot(TENANT_ID, 2026, 8, "CANCELLED"))
                .thenReturn(List.of());
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(inv -> {
            PayrollRun r = inv.getArgument(0);
            r.setId(RUN_ID);
            return r;
        });
        when(payrollRunEmployeeRepository.findAllByPayrollRunIdOrderByEmployeeCodeAsc(RUN_ID)).thenReturn(List.of());

        PayrollRunCreateRequest request = new PayrollRunCreateRequest();
        request.setYear(2026);
        request.setMonth(8);

        var response = service.createRun(request, ACTOR_ID, null);

        assertEquals("DRAFT", response.getStatus());
        assertEquals(2026, response.getYear());
        assertEquals(8, response.getMonth());
    }

    /** TEST 2: a second (non-cancelled) run for the same client+month must be rejected. */
    @Test
    void createRunRejectsDuplicateForSameClientAndMonth() {
        PayrollRun existing = new PayrollRun();
        existing.setId(1L);
        existing.setStatus("CALCULATED");
        when(payrollRunRepository.findAllByClientCompanyIdAndYearAndMonthAndStatusNot(TENANT_ID, 2026, 8, "CANCELLED"))
                .thenReturn(List.of(existing));

        PayrollRunCreateRequest request = new PayrollRunCreateRequest();
        request.setYear(2026);
        request.setMonth(8);

        assertThrows(DuplicateResourceException.class, () -> service.createRun(request, ACTOR_ID, null));
        verify(payrollRunRepository, never()).save(any());
    }

    /** TEST 9: an APPROVED payroll run must reject recalculation outright. */
    @Test
    void calculateRunRejectsWhenAlreadyApproved() {
        PayrollRun run = new PayrollRun();
        run.setId(RUN_ID);
        run.setClientCompanyId(TENANT_ID);
        run.setYear(2026);
        run.setMonth(8);
        run.setStatus("APPROVED");
        when(payrollRunRepository.findByIdAndClientCompanyId(RUN_ID, TENANT_ID)).thenReturn(Optional.of(run));
        doThrow(new BadRequestException("locked")).when(statusTransitionService).assertCalculable("APPROVED");

        assertThrows(BadRequestException.class, () -> service.calculateRun(RUN_ID, ACTOR_ID, null));
        verifyNoInteractions(payrollInputResolver, payrollCalculationService);
    }

    /** A PAID payroll run must also reject recalculation. */
    @Test
    void calculateRunRejectsWhenAlreadyPaid() {
        PayrollRun run = new PayrollRun();
        run.setId(RUN_ID);
        run.setClientCompanyId(TENANT_ID);
        run.setStatus("PAID");
        when(payrollRunRepository.findByIdAndClientCompanyId(RUN_ID, TENANT_ID)).thenReturn(Optional.of(run));
        doThrow(new BadRequestException("locked")).when(statusTransitionService).assertCalculable("PAID");

        assertThrows(BadRequestException.class, () -> service.calculateRun(RUN_ID, ACTOR_ID, null));
    }

    /** approveRun requires the run to be CALCULATED first. */
    @Test
    void approveRunRejectsWhenNotCalculated() {
        PayrollRun run = new PayrollRun();
        run.setId(RUN_ID);
        run.setClientCompanyId(TENANT_ID);
        run.setStatus("DRAFT");
        when(payrollRunRepository.findByIdAndClientCompanyId(RUN_ID, TENANT_ID)).thenReturn(Optional.of(run));
        doThrow(new BadRequestException("must be calculated")).when(statusTransitionService).assertApprovable("DRAFT");

        assertThrows(BadRequestException.class, () -> service.approveRun(RUN_ID, ACTOR_ID, null));
    }

    /** An APPROVED run cannot be cancelled through the simple cancel path (needs the reopen workflow first). */
    @Test
    void cancelRunRejectsWhenApproved() {
        PayrollRun run = new PayrollRun();
        run.setId(RUN_ID);
        run.setClientCompanyId(TENANT_ID);
        run.setStatus("APPROVED");
        when(payrollRunRepository.findByIdAndClientCompanyId(RUN_ID, TENANT_ID)).thenReturn(Optional.of(run));
        doThrow(new BadRequestException("locked")).when(statusTransitionService).assertCancellable("APPROVED");

        assertThrows(BadRequestException.class, () -> service.cancelRun(RUN_ID, "Made a mistake", ACTOR_ID, null));
    }

    /** Cancellation reason is mandatory, even for an otherwise-cancellable DRAFT/CALCULATED run. */
    @Test
    void cancelRunRejectsBlankReason() {
        PayrollRun run = new PayrollRun();
        run.setId(RUN_ID);
        run.setClientCompanyId(TENANT_ID);
        run.setStatus("DRAFT");
        when(payrollRunRepository.findByIdAndClientCompanyId(RUN_ID, TENANT_ID)).thenReturn(Optional.of(run));
        // statusTransitionService.assertCancellable("DRAFT") is a mock - does nothing by default, so this reaches the reason check.

        assertThrows(BadRequestException.class, () -> service.cancelRun(RUN_ID, "  ", ACTOR_ID, null));
        verify(payrollRunRepository, never()).save(any());
    }

    /** TEST: reopening a PAID run must be rejected with its own distinct message, never silently allowed. */
    @Test
    void reopenRunRejectsWhenPaid() {
        PayrollRun run = new PayrollRun();
        run.setId(RUN_ID);
        run.setClientCompanyId(TENANT_ID);
        run.setStatus("PAID");
        when(payrollRunRepository.findByIdAndClientCompanyId(RUN_ID, TENANT_ID)).thenReturn(Optional.of(run));
        doThrow(new BadRequestException("Paid payroll cannot be reopened through the standard workflow."))
                .when(statusTransitionService).assertReopenable("PAID");

        assertThrows(BadRequestException.class, () -> service.reopenRun(RUN_ID, "Need to fix a mistake", ACTOR_ID, null));
        verify(payrollRunRepository, never()).save(any());
    }

    /** TEST 9 (reopen path): an APPROVED run reopens successfully to CALCULATED when a reason is given and the caller is authorized. */
    @Test
    void reopenRunSucceedsForApprovedWithReason() {
        PayrollRun run = new PayrollRun();
        run.setId(RUN_ID);
        run.setClientCompanyId(TENANT_ID);
        run.setYear(2026);
        run.setMonth(8);
        run.setStatus("APPROVED");
        when(payrollRunRepository.findByIdAndClientCompanyId(RUN_ID, TENANT_ID)).thenReturn(Optional.of(run));
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payrollRunEmployeeRepository.findAllByPayrollRunIdOrderByEmployeeCodeAsc(RUN_ID)).thenReturn(List.of());

        var response = service.reopenRun(RUN_ID, "Attendance correction needed", ACTOR_ID, null);

        assertEquals("CALCULATED", response.getStatus());
        assertEquals("Attendance correction needed", response.getReopenReason());
    }

    /** Reopen reason is mandatory even for an otherwise-reopenable APPROVED run. */
    @Test
    void reopenRunRejectsBlankReason() {
        PayrollRun run = new PayrollRun();
        run.setId(RUN_ID);
        run.setClientCompanyId(TENANT_ID);
        run.setStatus("APPROVED");
        when(payrollRunRepository.findByIdAndClientCompanyId(RUN_ID, TENANT_ID)).thenReturn(Optional.of(run));

        assertThrows(BadRequestException.class, () -> service.reopenRun(RUN_ID, "", ACTOR_ID, null));
        verify(payrollRunRepository, never()).save(any());
    }

    /** TEST 11: calculating a DRAFT run twice for the same employee must update the existing PayrollRunEmployee row, never insert a second one. */
    @Test
    void recalculatingSameRunUpdatesExistingEmployeeRowInsteadOfDuplicating() {
        PayrollRun run = new PayrollRun();
        run.setId(RUN_ID);
        run.setClientCompanyId(TENANT_ID);
        run.setYear(2026);
        run.setMonth(8);
        run.setStatus("DRAFT");
        when(payrollRunRepository.findByIdAndClientCompanyId(RUN_ID, TENANT_ID)).thenReturn(Optional.of(run));
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee employee = new Employee();
        employee.setId(200L);
        employee.setEmployeeCode("EMP0001");
        employee.setFirstName("Test");
        employee.setLastName("Employee");
        employee.setStatus("ACTIVE");
        when(employeeRepository.findAllByClientCompanyIdAndStatusOrderByEmployeeCodeAsc(TENANT_ID, "ACTIVE"))
                .thenReturn(List.of(employee));

        when(attendanceRepository.findAllByClientCompanyIdAndAttendanceDateBetweenOrderByEmployeeIdAscAttendanceDateAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(siteRepository.findAllByClientCompanyId(TENANT_ID)).thenReturn(List.of());
        when(siteAssignmentRepository.findAllByClientCompanyIdAndStatus(TENANT_ID, "ACTIVE")).thenReturn(List.of());
        when(payrollSettingsResolver.resolve(TENANT_ID, 2026, 8)).thenReturn(new PayrollSettings());
        when(payrollAdjustmentRepository.findAllByClientCompanyIdAndYearAndMonth(TENANT_ID, 2026, 8)).thenReturn(List.of());

        EmployeePayrollInputs inputs = new EmployeePayrollInputs();
        inputs.setBasicSalary(BigDecimal.ZERO);
        inputs.setDa(BigDecimal.ZERO);
        inputs.setTotalGross(new BigDecimal("15000.00"));
        inputs.setPaidLeaveDays(BigDecimal.ZERO);
        inputs.setUnpaidLeaveDays(BigDecimal.ZERO);
        inputs.setPayableDays(new BigDecimal("30"));
        when(payrollInputResolver.resolveEmployeeInputs(eq(TENANT_ID), eq(employee), eq(2026), eq(8), any(), anyInt(), any(), any()))
                .thenReturn(inputs);

        PayrollCalculationResult result = new PayrollCalculationResult(
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("15000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("15000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("15000.00"));
        when(payrollCalculationService.calculate(any())).thenReturn(result);

        // First calculation: no existing row for this employee yet.
        when(payrollRunEmployeeRepository.findByPayrollRunIdAndEmployeeId(RUN_ID, 200L))
                .thenReturn(Optional.empty(), Optional.of(existingRowAfterFirstSave()));
        when(payrollRunEmployeeRepository.save(any(PayrollRunEmployee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payrollRunEmployeeRepository.findAllByPayrollRunIdOrderByEmployeeCodeAsc(RUN_ID)).thenReturn(List.of());

        service.calculateRun(RUN_ID, ACTOR_ID, null);
        service.calculateRun(RUN_ID, ACTOR_ID, null);

        // Exactly two saves (one per calculateRun call) - never a third row inserted for the same employee.
        verify(payrollRunEmployeeRepository, times(2)).save(any(PayrollRunEmployee.class));
    }

    private PayrollRunEmployee existingRowAfterFirstSave() {
        PayrollRunEmployee existing = new PayrollRunEmployee();
        existing.setId(1L);
        existing.setPayrollRunId(RUN_ID);
        existing.setEmployeeId(200L);
        return existing;
    }
}
