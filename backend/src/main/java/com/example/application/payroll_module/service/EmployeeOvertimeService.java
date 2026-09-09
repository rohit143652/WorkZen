package com.example.application.payroll_module.service;

import com.example.application.client_company_module.feature.FeatureAccessService;
import com.example.application.client_company_module.feature.FeatureCode;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.payroll_module.dto.OvertimeRecordRequest;
import com.example.application.payroll_module.dto.OvertimeRecordResponse;
import com.example.application.payroll_module.entity.EmployeeOvertimeRecord;
import com.example.application.payroll_module.repository.EmployeeOvertimeRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * The Overtime Register - one row per employee per date (see EmployeeOvertimeRecord/V107
 * migration for why this replaced a single monthly number). getTotalAmountForEmployeeMonth() is
 * what PayrollRunService actually reads for the Overtime line in Net Pay - everything else here
 * is the CRUD an Admin/Supervisor/HR uses to build that log up, day by day.
 */
@Service
public class EmployeeOvertimeService {

    private final EmployeeOvertimeRecordRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final TenantContextService tenantContext;
    private final FeatureAccessService featureAccessService;

    public EmployeeOvertimeService(EmployeeOvertimeRecordRepository repository, EmployeeRepository employeeRepository,
                                    UserRepository userRepository, TenantContextService tenantContext,
                                    FeatureAccessService featureAccessService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
        this.featureAccessService = featureAccessService;
    }

    /** The ACTOR's role label to store on the record - same mapping AttendanceService uses for its own createdByRole, kept consistent so an audit trail reads the same way across modules. */
    private String currentActorRoleLabel() {
        Set<String> roles = tenantContext.currentPrincipal().getRoleNames();
        if (roles.contains("HR_ADMIN")) return "HR_ADMIN";
        if (roles.contains("SITE_SUPERVISOR")) return "SITE_SUPERVISOR";
        if (roles.contains("CLIENT_ADMIN")) return "CLIENT_ADMIN";
        if (roles.contains("SITE_ADMIN")) return "SITE_ADMIN";
        return roles.stream().findFirst().orElse("ADMIN");
    }

    @Transactional
    public OvertimeRecordResponse markOvertime(OvertimeRecordRequest request, Long actorId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.OVERTIME_MANAGEMENT, "Overtime");

        Employee employee = employeeRepository.findByIdAndClientCompanyId(request.getEmployeeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        // Upsert by employee+date (matches the DB's own UNIQUE constraint) - re-marking the same
        // day corrects that day's entry rather than creating a duplicate, same pattern
        // AttendanceService/EmployeePaidLeaveBalance already use for their own per-day/per-month rows.
        EmployeeOvertimeRecord record = repository.findByEmployeeIdAndOvertimeDate(employee.getId(), request.getOvertimeDate())
                .orElseGet(EmployeeOvertimeRecord::new);
        record.setClientCompanyId(tenantId);
        record.setEmployeeId(employee.getId());
        record.setOvertimeDate(request.getOvertimeDate());
        record.setHours(request.getHours());
        record.setAmount(request.getAmount());
        record.setRemarks(request.getRemarks());
        record.setMarkedBy(actorId);
        record.setMarkedByRole(currentActorRoleLabel());

        return toResponse(repository.save(record), employee);
    }

    @Transactional
    public void deleteOvertime(Long id, Long employeeId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        featureAccessService.requireEnabledForCurrentTenant(FeatureCode.OVERTIME_MANAGEMENT, "Overtime");
        repository.findByClientCompanyIdAndIdAndEmployeeId(tenantId, id, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime record not found"));
        repository.deleteByClientCompanyIdAndIdAndEmployeeId(tenantId, id, employeeId);
    }

    @Transactional(readOnly = true)
    public List<OvertimeRecordResponse> getForEmployeeInRange(Long employeeId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("Start date must be on or before end date");
        }
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = employeeRepository.findByIdAndClientCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        return repository.findAllByClientCompanyIdAndEmployeeIdAndOvertimeDateBetweenOrderByOvertimeDateDesc(tenantId, employeeId, from, to)
                .stream().map(r -> toResponse(r, employee)).toList();
    }

    /**
     * What PayrollRunService actually reads for the Overtime line in Net Pay - the month's total
     * amount is ALWAYS the sum of the directly-entered per-entry amounts, never hours multiplied
     * by a fixed company-wide rate (see V108 migration for why that approach was dropped -
     * different days/situations can legitimately warrant different amounts). Hours is returned
     * alongside purely for display (e.g. "12.5 hrs" next to the rupee figure on a payslip/run
     * screen) - it plays no part in the amount calculation itself any more. Both are ZERO (not
     * an error) for a company with no OVERTIME_MANAGEMENT feature or no records - payroll
     * calculation should never fail just because overtime isn't in use.
     */
    @Transactional(readOnly = true)
    public MonthTotal getTotalsForEmployeeMonth(Long tenantId, Long employeeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<EmployeeOvertimeRecord> records = repository
                .findAllByClientCompanyIdAndEmployeeIdAndOvertimeDateBetweenOrderByOvertimeDateDesc(
                        tenantId, employeeId, ym.atDay(1), ym.atEndOfMonth());
        BigDecimal totalAmount = records.stream().map(EmployeeOvertimeRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHours = records.stream().map(EmployeeOvertimeRecord::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthTotal(totalAmount, totalHours);
    }

    /** Plain pair - avoids two separate queries (and thus two separate reads of the same rows) when a caller needs both figures together. */
    public record MonthTotal(BigDecimal amount, BigDecimal hours) {}

    private OvertimeRecordResponse toResponse(EmployeeOvertimeRecord r, Employee employee) {
        OvertimeRecordResponse dto = new OvertimeRecordResponse();
        dto.setId(r.getId());
        dto.setEmployeeId(r.getEmployeeId());
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setEmployeeName((employee.getFirstName() + " " + employee.getLastName()).trim());
        dto.setOvertimeDate(r.getOvertimeDate());
        dto.setHours(r.getHours());
        dto.setAmount(r.getAmount());
        dto.setRemarks(r.getRemarks());
        dto.setMarkedByRole(r.getMarkedByRole());
        if (r.getMarkedBy() != null) {
            userRepository.findById(r.getMarkedBy()).map(User::getUsername).ifPresent(dto::setMarkedByUsername);
        }
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}
