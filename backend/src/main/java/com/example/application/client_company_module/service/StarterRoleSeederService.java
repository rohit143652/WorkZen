package com.example.application.client_company_module.service;

import com.example.application.permission_module.entity.Permission;
import com.example.application.permission_module.repository.PermissionRepository;
import com.example.application.role_module.entity.Role;
import com.example.application.role_module.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every new client company used to start with NOTHING but the two system-wide roles
 * (CLIENT_ADMIN, CLIENT_USER) - a Client Admin had to hand-build every other role (HR, Site
 * Admin, Accountant, ...) and tick every permission themselves before their team could actually
 * use the app job-by-job. This seeds a sensible STARTING set of roles instead, each with
 * permissions matching what that role's name/description already implies (mirroring the same
 * roles the demo tenant has always shipped with - see V25__seed_business_roles.sql, which this
 * generalizes to run for every tenant instead of just the one demo company).
 *
 * These are only a starting point, not a permanent restriction - a Client Admin can rename,
 * delete, or re-permission any of them afterward exactly like any other custom role, or create
 * entirely new ones. SITE_READ is included wherever a role plausibly needs to see sites at all -
 * since there is no per-site visibility concept in this app (SITE_READ is all-or-nothing), that
 * automatically covers every future new site too, with nothing extra to configure per site.
 */
@Service
public class StarterRoleSeederService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public StarterRoleSeederService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public void seedStandardRoles(Long tenantId) {
        Map<String, RoleBlueprint> blueprints = new LinkedHashMap<>();

        blueprints.put("ADMIN", new RoleBlueprint(
                "Full administrative access within this company (tenant-scoped equivalent of Client Admin)",
                // Deliberately does NOT add ATTENDANCE_SELF_MARK here even though every other
                // role below gets it - an administrator manages the company, they don't
                // personally check in at a site, so this stays absent unless CLIENT_ADMIN
                // itself ever gains it (since this list is copied straight from CLIENT_ADMIN's
                // own current permissions, not hand-maintained separately).
                copyClientAdminPermissionNames()));

        blueprints.put("HR_ADMIN", new RoleBlueprint(
                "Manages employee records, login access, leave, and exits for this company",
                Set.of("DASHBOARD_VIEW", "PASSWORD_CHANGE",
                        "EMPLOYEE_READ", "EMPLOYEE_CREATE", "EMPLOYEE_UPDATE", "EMPLOYEE_ACTIVATE", "EMPLOYEE_DEACTIVATE",
                        "EMPLOYEE_ENABLE_LOGIN", "EMPLOYEE_DISABLE_LOGIN", "EMPLOYEE_RESET_PASSWORD", "EMPLOYEE_ASSIGN_ROLE",
                        "DEPARTMENT_READ", "DEPARTMENT_MANAGE", "DESIGNATION_READ", "DESIGNATION_MANAGE",
                        "SITE_READ", "EMPLOYEE_ASSIGNMENT_READ",
                        "ATTENDANCE_READ", "ATTENDANCE_SELF_MARK", "HOLIDAY_READ",
                        "LEAVE_REQUEST_READ", "LEAVE_REQUEST_MANAGE", "LEAVE_REQUEST_SELF_CREATE",
                        "EMPLOYEE_EXIT_READ", "EMPLOYEE_EXIT_CREATE", "EMPLOYEE_EXIT_SETTLE")));

        blueprints.put("SITE_ADMIN", new RoleBlueprint(
                "Manages sites and employee-to-site assignments for this company",
                Set.of("DASHBOARD_VIEW", "PASSWORD_CHANGE",
                        "SITE_CREATE", "SITE_READ", "SITE_UPDATE", "SITE_ACTIVATE", "SITE_DEACTIVATE",
                        "EMPLOYEE_READ", "EMPLOYEE_ASSIGN", "EMPLOYEE_TRANSFER", "EMPLOYEE_ASSIGNMENT_READ",
                        "ATTENDANCE_CREATE", "ATTENDANCE_READ", "ATTENDANCE_SELF_MARK", "LEAVE_REQUEST_SELF_CREATE")));

        blueprints.put("SITE_SUPERVISOR", new RoleBlueprint(
                "Day-to-day site operations - attendance, leave, and read-only visibility into employees/sites/assignments",
                Set.of("DASHBOARD_VIEW", "PASSWORD_CHANGE",
                        "EMPLOYEE_READ", "EMPLOYEE_ASSIGNMENT_READ", "SITE_READ",
                        "ATTENDANCE_CREATE", "ATTENDANCE_READ", "ATTENDANCE_SELF_MARK", "HOLIDAY_READ",
                        "LEAVE_REQUEST_READ", "LEAVE_REQUEST_MANAGE", "LEAVE_REQUEST_SELF_CREATE")));

        blueprints.put("ACCOUNTANT", new RoleBlueprint(
                "Read-only visibility for finance/reporting purposes",
                Set.of("DASHBOARD_VIEW", "PASSWORD_CHANGE",
                        "PAYROLL_RUN_READ", "PAYROLL_REGISTER_EXPORT", "SALARY_STRUCTURE_READ",
                        "ADVANCE_READ", "MONTHLY_PAYMENT_REPORT_EXPORT", "ATTENDANCE_SELF_MARK", "LEAVE_REQUEST_SELF_CREATE")));

        for (Map.Entry<String, RoleBlueprint> entry : blueprints.entrySet()) {
            createRoleIfAbsent(tenantId, entry.getKey(), entry.getValue());
        }
    }

    private void createRoleIfAbsent(Long tenantId, String name, RoleBlueprint blueprint) {
        if (roleRepository.existsByClientCompanyIdAndNameIgnoreCase(tenantId, name)) return;

        Role role = new Role();
        role.setClientCompanyId(tenantId);
        role.setName(name);
        role.setDescription(blueprint.description());

        List<Permission> permissions = permissionRepository.findAllByNameIn(blueprint.permissionNames());
        role.setPermissions(new HashSet<>(permissions));

        roleRepository.save(role);
    }

    /** ADMIN is meant to be the tenant-scoped equivalent of Client Admin, so rather than hand-listing every permission a second time (and risking the two lists drifting apart later), it just copies whatever the system CLIENT_ADMIN role currently has. */
    private Set<String> copyClientAdminPermissionNames() {
        return roleRepository.findByClientCompanyIdIsNullAndName("CLIENT_ADMIN")
                .map(clientAdmin -> clientAdmin.getPermissions().stream().map(Permission::getName).collect(java.util.stream.Collectors.toSet()))
                .orElse(Set.of());
    }

    private record RoleBlueprint(String description, Set<String> permissionNames) {}
}
