package com.example.application.employee_assignment_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_assignment_module.dto.*;
import com.example.application.employee_assignment_module.entity.EmployeeSiteAssignment;
import com.example.application.employee_assignment_module.repository.EmployeeSiteAssignmentRepository;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.site_module.entity.Site;
import com.example.application.site_module.service.SiteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Every method operates within the caller's own tenant (resolved once via
 * TenantContextService) and every Employee/Site lookup goes through the
 * tenant-scoped repository/service methods, so an assignment can never be
 * created that mixes an Employee and a Site from different tenants - even
 * if both IDs happen to be individually valid within their own tenant.
 */
@Service
public class EmployeeAssignmentService {

    private final EmployeeSiteAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SiteService siteService;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public EmployeeAssignmentService(EmployeeSiteAssignmentRepository assignmentRepository,
                                      EmployeeRepository employeeRepository, SiteService siteService,
                                      TenantContextService tenantContext, AuditService auditService) {
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
        this.siteService = siteService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeAssignmentResponse> findAll(Pageable pageable) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return assignmentRepository.findAllByClientCompanyId(tenantId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeAssignmentResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<EmployeeAssignmentResponse> findByEmployee(Long employeeId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        // Tenant check happens implicitly: the employee lookup below throws if it's not this tenant's.
        getTenantEmployee(employeeId);
        return assignmentRepository.findAllByEmployeeIdAndClientCompanyIdOrderByStartDateDesc(employeeId, tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeAssignmentResponse> findActiveBySite(Long siteId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        siteService.getEntityForCurrentTenant(siteId); // tenant check
        return assignmentRepository.findAllBySiteIdAndClientCompanyIdAndStatus(siteId, tenantId, "ACTIVE")
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmployeeAssignmentResponse create(EmployeeAssignmentRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = getTenantEmployee(request.getEmployeeId());
        Site site = siteService.getEntityForCurrentTenant(request.getSiteId());

        if (request.isPrimary()) {
            var existingOpt = assignmentRepository.findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(
                    employee.getId(), tenantId, "ACTIVE");

            if (existingOpt.isPresent()) {
                EmployeeSiteAssignment existing = existingOpt.get();
                if (existing.getSiteId().equals(site.getId())) {
                    throw new BadRequestException(
                            "Employee " + employee.getEmployeeCode() + " is already assigned to this site.");
                }
                // Employee is currently assigned elsewhere: automatically end that
                // assignment and continue below to create the new one, exactly like
                // Transfer does. This keeps "assign to a different site" a single
                // action from the UI's point of view instead of requiring the
                // caller to explicitly end the old assignment first.
                existing.setStatus("ENDED");
                existing.setEndDate(request.getStartDate().minusDays(1));
                existing.setUpdatedBy(actorId);
                assignmentRepository.save(existing);
                auditService.log(actorId, "EMPLOYEE_UNASSIGNED",
                        "Ended previous assignment for employee " + employee.getEmployeeCode()
                                + " (site " + existing.getSiteId() + ") due to reassignment", httpRequest);
            }
        }

        checkAllocationCapacity(site, 1);

        EmployeeSiteAssignment assignment = new EmployeeSiteAssignment();
        assignment.setClientCompanyId(tenantId);
        assignment.setEmployeeId(employee.getId());
        assignment.setSiteId(site.getId());
        assignment.setAssignmentType(request.getAssignmentType() == null ? "REGULAR" : request.getAssignmentType());
        assignment.setStartDate(request.getStartDate());
        assignment.setPrimary(request.isPrimary());
        assignment.setStatus("ACTIVE");
        assignment.setRemarks(request.getRemarks());
        assignment.setCreatedBy(actorId);

        EmployeeSiteAssignment saved = assignmentRepository.save(assignment);
        auditService.log(actorId, "EMPLOYEE_ASSIGNED",
                "Assigned employee " + employee.getEmployeeCode() + " to site " + site.getSiteCode(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public BulkAssignmentResult bulkAssign(BulkEmployeeAssignmentRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Site site = siteService.getEntityForCurrentTenant(request.getSiteId());

        List<String> rejected = new ArrayList<>();
        int assignedCount = 0;
        long currentlyAssigned = assignmentRepository.countBySiteIdAndClientCompanyIdAndStatus(site.getId(), tenantId, "ACTIVE");

        for (Long employeeId : request.getEmployeeIds()) {
            Employee employee;
            try {
                employee = getTenantEmployee(employeeId);
            } catch (TenantAccessDeniedException ex) {
                rejected.add("Employee " + employeeId + ": not found in this tenant");
                continue;
            }

            boolean alreadyAssigned = assignmentRepository
                    .findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(employeeId, tenantId, "ACTIVE")
                    .isPresent();
            if (alreadyAssigned) {
                // Deliberately different from single create(): bulk-assign is for filling new
                // headcount at a site, not mass-reassigning existing staff, so an already-assigned
                // employee is skipped/reported here rather than auto-transferred. To move an
                // already-assigned employee, use the single assignment endpoint (auto-transfers)
                // or POST /api/employees/{id}/transfer explicitly.
                rejected.add("Employee " + employee.getEmployeeCode() + ": already has an active assignment");
                continue;
            }

            if (site.getRequiredEmployeeCount() > 0 && !site.isAllowOverAllocation()
                    && currentlyAssigned + 1 > site.getRequiredEmployeeCount()) {
                rejected.add("Employee " + employee.getEmployeeCode() + ": site is at required capacity ("
                        + site.getRequiredEmployeeCount() + ")");
                continue;
            }

            EmployeeSiteAssignment assignment = new EmployeeSiteAssignment();
            assignment.setClientCompanyId(tenantId);
            assignment.setEmployeeId(employee.getId());
            assignment.setSiteId(site.getId());
            assignment.setAssignmentType("REGULAR");
            assignment.setStartDate(request.getStartDate());
            assignment.setPrimary(true);
            assignment.setStatus("ACTIVE");
            assignment.setRemarks(request.getRemarks());
            assignment.setCreatedBy(actorId);
            assignmentRepository.save(assignment);

            currentlyAssigned++;
            assignedCount++;
        }

        auditService.log(actorId, "BULK_EMPLOYEES_ASSIGNED",
                assignedCount + " of " + request.getEmployeeIds().size() + " employees assigned to site " + site.getSiteCode(),
                httpRequest);
        return new BulkAssignmentResult(request.getEmployeeIds().size(), assignedCount, rejected);
    }

    @Transactional
    public EmployeeAssignmentResponse transfer(Long employeeId, TransferEmployeeRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        Employee employee = getTenantEmployee(employeeId);
        Site toSite = siteService.getEntityForCurrentTenant(request.getToSiteId());

        checkAllocationCapacity(toSite, 1);

        assignmentRepository.findFirstByEmployeeIdAndClientCompanyIdAndStatusOrderByStartDateDesc(employeeId, tenantId, "ACTIVE")
                .ifPresent(current -> {
                    current.setStatus("ENDED");
                    current.setEndDate(request.getEffectiveDate().minusDays(1));
                    current.setUpdatedBy(actorId);
                    assignmentRepository.save(current);
                });

        EmployeeSiteAssignment newAssignment = new EmployeeSiteAssignment();
        newAssignment.setClientCompanyId(tenantId);
        newAssignment.setEmployeeId(employee.getId());
        newAssignment.setSiteId(toSite.getId());
        newAssignment.setAssignmentType("REGULAR");
        newAssignment.setStartDate(request.getEffectiveDate());
        newAssignment.setPrimary(true);
        newAssignment.setStatus("ACTIVE");
        newAssignment.setRemarks(request.getReason());
        newAssignment.setCreatedBy(actorId);
        EmployeeSiteAssignment saved = assignmentRepository.save(newAssignment);

        auditService.log(actorId, "EMPLOYEE_TRANSFERRED",
                "Transferred employee " + employee.getEmployeeCode() + " to site " + toSite.getSiteCode(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public EmployeeAssignmentResponse end(Long id, Long actorId, HttpServletRequest httpRequest) {
        EmployeeSiteAssignment assignment = getEntity(id);
        assignment.setStatus("ENDED");
        if (assignment.getEndDate() == null) {
            assignment.setEndDate(LocalDate.now());
        }
        assignment.setUpdatedBy(actorId);
        EmployeeSiteAssignment saved = assignmentRepository.save(assignment);
        auditService.log(actorId, "EMPLOYEE_UNASSIGNED", "Ended assignment " + id, httpRequest);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeAssignmentResponse> findActiveForTenant() {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return assignmentRepository.findAllByClientCompanyIdAndStatus(tenantId, "ACTIVE")
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public BulkEndResult bulkEnd(BulkEndAssignmentRequest request, Long actorId, HttpServletRequest httpRequest) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        List<String> failed = new ArrayList<>();
        int endedCount = 0;

        for (Long assignmentId : request.getAssignmentIds()) {
            EmployeeSiteAssignment assignment = assignmentRepository
                    .findByIdAndClientCompanyId(assignmentId, tenantId).orElse(null);
            if (assignment == null) {
                failed.add("Assignment " + assignmentId + ": not found in this tenant");
                continue;
            }
            if (!"ACTIVE".equals(assignment.getStatus())) {
                failed.add("Assignment " + assignmentId + ": already ended");
                continue;
            }
            assignment.setStatus("ENDED");
            assignment.setEndDate(LocalDate.now());
            assignment.setUpdatedBy(actorId);
            assignmentRepository.save(assignment);
            endedCount++;
        }

        auditService.log(actorId, "EMPLOYEE_UNASSIGNED",
                endedCount + " of " + request.getAssignmentIds().size() + " assignments ended in bulk", httpRequest);
        return new BulkEndResult(request.getAssignmentIds().size(), endedCount, failed);
    }

    private void checkAllocationCapacity(Site site, int additional) {
        if (site.getRequiredEmployeeCount() <= 0 || site.isAllowOverAllocation()) {
            return; // no cap configured, or over-allocation explicitly permitted
        }
        long current = assignmentRepository.countBySiteIdAndClientCompanyIdAndStatus(
                site.getId(), site.getClientCompanyId(), "ACTIVE");
        if (current + additional > site.getRequiredEmployeeCount()) {
            throw new BadRequestException("Site " + site.getSiteCode() + " requires " + site.getRequiredEmployeeCount()
                    + " employees and already has " + current + " assigned. Enable over-allocation on the site to proceed anyway.");
        }
    }

    private Employee getTenantEmployee(Long employeeId) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return employeeRepository.findByIdAndClientCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Employee " + employeeId + " does not belong to the current tenant"));
    }

    private EmployeeSiteAssignment getEntity(Long id) {
        Long tenantId = tenantContext.requireCurrentTenantId();
        return assignmentRepository.findByIdAndClientCompanyId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Assignment " + id + " does not belong to the current tenant"));
    }

    private EmployeeAssignmentResponse toResponse(EmployeeSiteAssignment a) {
        EmployeeAssignmentResponse r = new EmployeeAssignmentResponse();
        r.setId(a.getId());
        r.setEmployeeId(a.getEmployeeId());
        employeeRepository.findById(a.getEmployeeId()).ifPresent(e -> {
            r.setEmployeeCode(e.getEmployeeCode());
            r.setEmployeeName(e.getFirstName() + " " + e.getLastName());
        });
        r.setSiteId(a.getSiteId());
        try {
            Site site = siteService.getEntityForCurrentTenant(a.getSiteId());
            r.setSiteName(site.getSiteName());
        } catch (TenantAccessDeniedException ignored) {
            r.setSiteName(null);
        }
        r.setAssignmentType(a.getAssignmentType());
        r.setStartDate(a.getStartDate());
        r.setEndDate(a.getEndDate());
        r.setPrimary(a.isPrimary());
        r.setStatus(a.getStatus());
        r.setRemarks(a.getRemarks());
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }
}
