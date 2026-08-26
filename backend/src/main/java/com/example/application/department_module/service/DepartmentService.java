package com.example.application.department_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.department_module.dto.DepartmentRequest;
import com.example.application.department_module.dto.DepartmentResponse;
import com.example.application.department_module.entity.Department;
import com.example.application.department_module.repository.DepartmentRepository;
import com.example.application.employee_module.repository.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Any authenticated tenant user with DEPARTMENT_READ can list departments
 * (needed to populate the Employee form dropdown); only DEPARTMENT_MANAGE
 * can add/rename/activate/deactivate them. CLIENT_ADMIN has both by default
 * grant - see V21 migration - so a Client Admin can add new departments
 * on the fly without SUPER_ADMIN involvement.
 */
@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                              TenantContextService tenantContext, AuditService auditService) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll(boolean includeInactive) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        List<Department> list = includeInactive
                ? departmentRepository.findAllByClientCompanyIdOrderByNameAsc(tenantId)
                : departmentRepository.findAllByClientCompanyIdAndStatusOrderByNameAsc(tenantId, "ACTIVE");
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        String name = request.getName().trim();
        if (departmentRepository.existsByClientCompanyIdAndNameIgnoreCase(tenantId, name)) {
            throw new DuplicateResourceException("Department already exists: " + name);
        }
        Department department = new Department();
        department.setClientCompanyId(tenantId);
        department.setName(name);
        department.setCreatedBy(actorId);
        Department saved = departmentRepository.save(department);
        auditService.log(actorId, "DEPARTMENT_CREATED", "Added department " + saved.getName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse rename(Long id, DepartmentRequest request, Long actorId, HttpServletRequest httpRequest) {
        Department department = getEntity(id);
        String name = request.getName().trim();
        if (!department.getName().equalsIgnoreCase(name)
                && departmentRepository.existsByClientCompanyIdAndNameIgnoreCase(department.getClientCompanyId(), name)) {
            throw new DuplicateResourceException("Department already exists: " + name);
        }
        department.setName(name);
        Department saved = departmentRepository.save(department);
        auditService.log(actorId, "DEPARTMENT_UPDATED", "Renamed department to " + saved.getName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse setStatus(Long id, String status, Long actorId, HttpServletRequest httpRequest) {
        Department department = getEntity(id);
        department.setStatus(status);
        Department saved = departmentRepository.save(department);
        auditService.log(actorId, "DEPARTMENT_UPDATED",
                "Department " + saved.getName() + " status set to " + status, httpRequest);
        return toResponse(saved);
    }

    /** Used by EmployeeService to validate a submitted department name belongs to the current tenant. */
    @Transactional(readOnly = true)
    public boolean existsForCurrentTenant(String name) {
        Long tenantId = tenantContext.currentTenantIdOrNull();
        if (tenantId == null || name == null) return true; // SUPER_ADMIN/house context: no master-list enforcement
        return departmentRepository.existsByClientCompanyIdAndNameIgnoreCase(tenantId, name.trim());
    }

    private Department getEntity(Long id) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return departmentRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Department " + id + " does not belong to the current tenant"));
    }

    private DepartmentResponse toResponse(Department d) {
        long count = employeeRepository.countByClientCompanyIdAndDepartmentIgnoreCase(d.getClientCompanyId(), d.getName());
        return new DepartmentResponse(d.getId(), d.getName(), d.getStatus(), count);
    }
}
