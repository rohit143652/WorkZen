package com.example.application.salary_structure_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.salary_structure_module.dto.AssignSalaryStructureRequest;
import com.example.application.salary_structure_module.dto.EmployeeSalaryStructureResponse;
import com.example.application.salary_structure_module.entity.EmployeeSalaryStructure;
import com.example.application.salary_structure_module.entity.SalaryStructure;
import com.example.application.salary_structure_module.repository.EmployeeSalaryStructureRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Employee <-> Salary Structure assignment, mirroring the exact "never
 * overwrite, always end the old row and start a new one" pattern already
 * used for EmployeeSiteAssignment: assigning a new structure ends the
 * previous ACTIVE row (effectiveTo = day before the new effectiveFrom)
 * instead of mutating it, so a past payroll period can always resolve
 * exactly which structure applied on that date (spec sections 14, 61).
 */
@Service
public class EmployeeSalaryStructureService {

    private final EmployeeSalaryStructureRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryStructureService salaryStructureService;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public EmployeeSalaryStructureService(EmployeeSalaryStructureRepository assignmentRepository,
                                           EmployeeRepository employeeRepository,
                                           SalaryStructureService salaryStructureService,
                                           TenantContextService tenantContext, AuditService auditService) {
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
        this.salaryStructureService = salaryStructureService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EmployeeSalaryStructureResponse> findHistory(Long employeeId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        getTenantEmployee(employeeId); // tenant check
        return assignmentRepository
                .findAllByClientCompanyIdAndEmployeeIdOrderByEffectiveFromDesc(tenantId, employeeId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeSalaryStructureResponse findCurrent(Long employeeId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        getTenantEmployee(employeeId); // tenant check
        return assignmentRepository
                .findFirstByClientCompanyIdAndEmployeeIdAndStatusOrderByEffectiveFromDesc(tenantId, employeeId, "ACTIVE")
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public EmployeeSalaryStructureResponse assign(Long employeeId, AssignSalaryStructureRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = getTenantEmployee(employeeId);
        SalaryStructure structure = salaryStructureService.getEntityForCurrentTenant(request.getSalaryStructureId());

        if (!"ACTIVE".equals(structure.getStatus())) {
            throw new BadRequestException("Cannot assign an inactive salary structure");
        }

        assignmentRepository.findFirstByClientCompanyIdAndEmployeeIdAndStatusOrderByEffectiveFromDesc(tenantId, employeeId, "ACTIVE")
                .ifPresent(current -> {
                    if (!request.getEffectiveFrom().isAfter(current.getEffectiveFrom())) {
                        throw new BadRequestException(
                                "New effective date must be after the current assignment's effective date (" + current.getEffectiveFrom() + ")");
                    }
                    current.setStatus("ENDED");
                    current.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
                    assignmentRepository.save(current);
                });

        EmployeeSalaryStructure assignment = new EmployeeSalaryStructure();
        assignment.setClientCompanyId(tenantId);
        assignment.setEmployeeId(employee.getId());
        assignment.setSalaryStructureId(structure.getId());
        assignment.setEffectiveFrom(request.getEffectiveFrom());
        assignment.setStatus("ACTIVE");
        assignment.setCreatedBy(actorId);
        EmployeeSalaryStructure saved = assignmentRepository.save(assignment);

        auditService.log(actorId, "SALARY_ASSIGNED",
                "Assigned salary structure " + structure.getStructureName() + " to employee " + employee.getEmployeeCode()
                        + " effective " + request.getEffectiveFrom(), httpRequest);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Spec sections 53/54 - "clean service methods future Payroll can
    // consume" + "August payroll must still use August salary". These are
    // read-only, date-aware lookups; Payroll Processing (not built yet)
    // will call these instead of reaching into these tables directly.
    // ------------------------------------------------------------------

    /** Returns the salary structure assignment (if any) that was in force for this employee on payrollDate. */
    @Transactional(readOnly = true)
    public Optional<EmployeeSalaryStructureResponse> getActiveSalaryStructure(Long employeeId, LocalDate payrollDate) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        getTenantEmployee(employeeId); // tenant check
        return assignmentRepository.findEffectiveOn(tenantId, employeeId, payrollDate).map(this::toResponse);
    }

    /** Returns the resolved component line items (code, type, calculationType, resolved amount) effective on payrollDate. */
    @Transactional(readOnly = true)
    public List<com.example.application.salary_structure_module.dto.SalaryStructureComponentResponse> getSalaryComponents(Long employeeId, LocalDate payrollDate) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        getTenantEmployee(employeeId); // tenant check
        return assignmentRepository.findEffectiveOn(tenantId, employeeId, payrollDate)
                .map(a -> salaryStructureService.findById(a.getSalaryStructureId()).getComponents())
                .orElse(List.of());
    }

    /** Returns the gross earnings figure effective on payrollDate, or null if the employee had no salary structure on that date. */
    @Transactional(readOnly = true)
    public java.math.BigDecimal calculateGrossSalary(Long employeeId, LocalDate payrollDate) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        getTenantEmployee(employeeId); // tenant check
        return assignmentRepository.findEffectiveOn(tenantId, employeeId, payrollDate)
                .map(a -> salaryStructureService.findById(a.getSalaryStructureId()).getGrossEarnings())
                .orElse(null);
    }

    private Employee getTenantEmployee(Long employeeId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return employeeRepository.findByIdAndClientCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Employee " + employeeId + " does not belong to the current tenant"));
    }

    private EmployeeSalaryStructureResponse toResponse(EmployeeSalaryStructure a) {
        var structureResponse = salaryStructureService.findById(a.getSalaryStructureId());

        EmployeeSalaryStructureResponse r = new EmployeeSalaryStructureResponse();
        r.setId(a.getId());
        r.setSalaryStructureId(a.getSalaryStructureId());
        r.setStructureCode(structureResponse.getStructureCode());
        r.setStructureName(structureResponse.getStructureName());
        r.setSalaryType(structureResponse.getSalaryType());
        r.setEffectiveFrom(a.getEffectiveFrom());
        r.setEffectiveTo(a.getEffectiveTo());
        r.setStatus(a.getStatus());
        r.setGrossEarnings(structureResponse.getGrossEarnings());
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }
}
