package com.example.application.employee_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.employee_module.dto.ExtraPermissionsRequest;
import com.example.application.employee_module.dto.ExtraPermissionsResponse;
import com.example.application.employee_module.dto.PermissionOption;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.entity.User;
import com.example.application.permission_module.entity.Permission;
import com.example.application.permission_module.repository.PermissionRepository;
import com.example.application.role_module.entity.Role;
import com.example.application.role_module.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Gives ONE employee extra permissions on top of their role - for the case where two people share
 * a role but one of them needs something more, without inventing a new role for that one person.
 *
 * Strictly additive: an employee's effective permissions are (their role's) UNION (these extras);
 * nothing here can take away what the role gives. The union itself is computed in exactly one place
 * (CustomUserPrincipal), which both the server's authorization checks and the permission list sent
 * to the browser read from - so a grant takes effect on the employee's very next request.
 *
 * The same guardrails as editing a role (RoleService), because this is just another way of handing
 * out access:
 *   - You can only grant permissions you hold yourself (Super Admin excepted; PAYSLIP_SELF_VIEW is
 *     exempt exactly as it is for roles). Extras you hold count as held, so access can't be laundered
 *     upward by chaining grants.
 *   - A permission you don't hold that someone already has is left alone - neither addable nor
 *     removable by you.
 *   - You can only touch employees of your own company.
 *   - You can't change your own additional permissions.
 */
@Service
public class EmployeeExtraPermissionService {

    private final EmployeeRepository employeeRepository;
    private final PermissionRepository permissionRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public EmployeeExtraPermissionService(EmployeeRepository employeeRepository, PermissionRepository permissionRepository,
                                          TenantContextService tenantContext, AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.permissionRepository = permissionRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ExtraPermissionsResponse view(Long employeeId) {
        Employee employee = loadEmployeeWithLogin(employeeId);
        return build(employee, employee.getUser(), tenantContext.isSuperAdmin(), tenantContext.currentPermissionNames());
    }

    @Transactional
    public ExtraPermissionsResponse update(Long employeeId, ExtraPermissionsRequest request, Long actorId, HttpServletRequest httpRequest) {
        Employee employee = loadEmployeeWithLogin(employeeId);
        User target = employee.getUser();
        boolean superAdmin = tenantContext.isSuperAdmin();
        Set<String> actorPermissions = tenantContext.currentPermissionNames();

        if (!superAdmin && target.getId() != null && target.getId().equals(actorId)) {
            throw new BadRequestException("You cannot change your own additional permissions - ask another admin to do it.");
        }

        Set<Long> ids = request.permissionIds() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.permissionIds());
        Set<Permission> requested = ids.isEmpty() ? new HashSet<>() : new HashSet<>(permissionRepository.findAllById(ids));
        if (requested.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more permission IDs do not exist");
        }

        Set<String> disallowed = requested.stream().map(Permission::getName)
                .filter(name -> !manageable(name, superAdmin, actorPermissions))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (!disallowed.isEmpty()) {
            throw new BadRequestException("You cannot grant permissions you do not have: " + String.join(", ", disallowed));
        }

        Map<String, Permission> fromRole = rolePermissions(target);
        Set<String> redundant = requested.stream().map(Permission::getName).filter(fromRole::containsKey)
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (!redundant.isEmpty()) {
            throw new BadRequestException("Already given by this employee's role, so there is nothing to add: " + String.join(", ", redundant));
        }

        // The new set = what was asked for + whatever is currently granted that THIS editor isn't allowed to touch.
        Set<Permission> result = new HashSet<>(requested);
        for (Permission current : target.getExtraPermissions()) {
            if (!manageable(current.getName(), superAdmin, actorPermissions)) {
                result.add(current);
            }
        }

        Set<String> before = target.getExtraPermissions().stream().map(Permission::getName).collect(Collectors.toCollection(java.util.TreeSet::new));
        Set<String> after = result.stream().map(Permission::getName).collect(Collectors.toCollection(java.util.TreeSet::new));
        Set<String> added = new java.util.TreeSet<>(after);
        added.removeAll(before);
        Set<String> removed = new java.util.TreeSet<>(before);
        removed.removeAll(after);

        if (!added.isEmpty() || !removed.isEmpty()) {
            // Mutated in place (not replaced) so only the rows that actually changed are written.
            target.getExtraPermissions().retainAll(result);
            target.getExtraPermissions().addAll(result);
            auditService.log(actorId, "USER_EXTRA_PERMISSIONS_UPDATED",
                    "Additional permissions changed for employee " + employee.getEmployeeCode() + " (login " + target.getUsername() + ")"
                            + (added.isEmpty() ? "" : ", added: " + String.join(", ", added))
                            + (removed.isEmpty() ? "" : ", removed: " + String.join(", ", removed)),
                    httpRequest);
        }
        return build(employee, target, superAdmin, actorPermissions);
    }

    // ------------------------------------------------------------------ helpers

    /** Tenant-scoped: another company's employee is "not found", exactly as everywhere else. A Super Admin has no company and sees any. */
    private Employee loadEmployeeWithLogin(Long employeeId) {
        Employee employee = tenantContext.isSuperAdmin()
                ? employeeRepository.findById(employeeId).orElse(null)
                : employeeRepository.findByIdAndClientCompanyId(employeeId, tenantContext.requireCurrentTenantId()).orElse(null);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        }
        if (!employee.hasLogin()) {
            throw new BadRequestException("This employee has no login account, so there is nothing to give permissions to. Enable login first.");
        }
        return employee;
    }

    /** May the person editing add/remove this permission? */
    private static boolean manageable(String permissionName, boolean superAdmin, Set<String> actorPermissions) {
        return superAdmin || actorPermissions.contains(permissionName) || RoleService.UNRESTRICTED_PERMISSIONS.contains(permissionName);
    }

    private static Map<String, Permission> rolePermissions(User user) {
        Map<String, Permission> byName = new TreeMap<>();
        for (Role role : user.getRoles()) {
            for (Permission permission : role.getPermissions()) {
                byName.put(permission.getName(), permission);
            }
        }
        return byName;
    }

    private ExtraPermissionsResponse build(Employee employee, User target, boolean superAdmin, Set<String> actorPermissions) {
        Map<String, Permission> fromRole = rolePermissions(target);
        Set<Long> extraIds = target.getExtraPermissions().stream().map(Permission::getId).collect(Collectors.toSet());
        Comparator<Permission> byName = Comparator.comparing(Permission::getName);

        List<PermissionOption> roleOptions = fromRole.values().stream()
                .map(p -> option(p, false)).toList();
        List<PermissionOption> extraOptions = target.getExtraPermissions().stream().sorted(byName)
                .map(p -> option(p, !manageable(p.getName(), superAdmin, actorPermissions))).toList();

        List<PermissionOption> grantable = new ArrayList<>();
        for (Permission p : permissionRepository.findAll().stream().filter(Permission::isActive).sorted(byName).toList()) {
            if (fromRole.containsKey(p.getName())) continue;                       // the role already gives it
            boolean canManage = manageable(p.getName(), superAdmin, actorPermissions);
            if (canManage || extraIds.contains(p.getId())) {                        // a current extra is shown even if locked
                grantable.add(option(p, !canManage));
            }
        }
        List<String> roleNames = target.getRoles().stream().map(Role::getName).sorted().toList();
        return new ExtraPermissionsResponse(employee.getId(), target.getUsername(), roleNames, roleOptions, extraOptions, grantable);
    }

    private static PermissionOption option(Permission p, boolean locked) {
        return new PermissionOption(p.getId(), p.getName(), p.getDescription(), locked);
    }
}
