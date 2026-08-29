package com.example.application.role_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.exception.TenantAccessDeniedException;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.permission_module.entity.Permission;
import com.example.application.permission_module.repository.PermissionRepository;
import com.example.application.role_module.dto.RolePermissionsRequest;
import com.example.application.role_module.dto.RoleRequest;
import com.example.application.role_module.dto.RoleResponse;
import com.example.application.role_module.entity.Role;
import com.example.application.role_module.repository.RoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SECURITY-CRITICAL: this service is the single place role visibility,
 * ownership, and grantable permissions are enforced for non-SUPER_ADMIN
 * callers (Client Admins). Two guardrails apply everywhere a non-SUPER_ADMIN
 * creates/updates/assigns a role:
 *
 *  1. OWNERSHIP - a tenant user may only create/update/delete/assign roles
 *     that belong to their own tenant (Role.clientCompanyId == their own
 *     client_company_id). Global/house roles (SUPER_ADMIN, ADMIN, MANAGER,
 *     USER, CLIENT) are read-only to tenant users, except for a small,
 *     explicit safelist (CLIENT_ADMIN, CLIENT_USER) offered as assignable
 *     defaults for brand-new tenants that haven't created their own roles
 *     yet.
 *  2. PERMISSION CEILING - a tenant user can never attach a permission to
 *     ANY role that they do not themselves currently hold. Without this,
 *     a Client Admin could create a custom role bundled with, say,
 *     CLIENT_COMPANY_CREATE and assign it to an employee, instantly
 *     escalating that employee to platform-owner-level access - tenant
 *     scoping of the ROLE row alone does not scope the PERMISSION's real
 *     effect, since @PreAuthorize only checks the authority name.
 */
@Service
public class RoleService {

    /** Global roles a brand-new tenant (with no custom roles yet) may still assign to their own employees. */
    private static final Set<String> TENANT_ASSIGNABLE_GLOBAL_ROLES = Set.of("CLIENT_ADMIN", "CLIENT_USER");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantContextService tenantContext;
    private final AuditService auditService;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                        TenantContextService tenantContext, AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        if (tenantContext.isSuperAdmin()) {
            return roleRepository.findAll().stream().map(this::toResponse).toList();
        }
        Long tenantId = tenantContext.requireCurrentTenantId();
        List<Role> visible = new ArrayList<>(roleRepository.findAllByClientCompanyId(tenantId));
        visible.addAll(roleRepository.findAllByClientCompanyIdIsNullAndNameIn(TENANT_ASSIGNABLE_GLOBAL_ROLES));
        return visible.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        return toResponse(getVisibleEntity(id));
    }

    @Transactional
    public RoleResponse create(RoleRequest request, Long actorId, HttpServletRequest httpRequest) {
        boolean superAdmin = tenantContext.isSuperAdmin();
        Long tenantId = superAdmin ? null : tenantContext.requireCurrentTenantId();

        boolean nameTaken = tenantId != null
                ? roleRepository.existsByClientCompanyIdAndNameIgnoreCase(tenantId, request.getName())
                : roleRepository.existsByClientCompanyIdIsNullAndNameIgnoreCase(request.getName());
        if (nameTaken) {
            throw new DuplicateResourceException("Role already exists: " + request.getName());
        }

        Role role = new Role();
        role.setClientCompanyId(tenantId);
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setPermissions(resolvePermissionsWithCeiling(request.getPermissionIds(), superAdmin));
        Role saved = roleRepository.save(role);
        auditService.log(actorId, "ROLE_CREATED", "Created role " + saved.getName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request, Long actorId, Set<String> actorRoleNames, HttpServletRequest httpRequest) {
        boolean superAdmin = tenantContext.isSuperAdmin();
        Role role = getOwnedEntity(id, superAdmin);
        rejectIfEditingOwnRole(role, superAdmin, actorRoleNames);

        boolean nameChanged = !role.getName().equalsIgnoreCase(request.getName());
        if (nameChanged) {
            boolean nameTaken = role.getClientCompanyId() != null
                    ? roleRepository.existsByClientCompanyIdAndNameIgnoreCase(role.getClientCompanyId(), request.getName())
                    : roleRepository.existsByClientCompanyIdIsNullAndNameIgnoreCase(request.getName());
            if (nameTaken) {
                throw new DuplicateResourceException("Role already exists: " + request.getName());
            }
        }
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        if (request.getPermissionIds() != null) {
            role.setPermissions(resolvePermissionsWithCeiling(request.getPermissionIds(), superAdmin));
        }
        Role saved = roleRepository.save(role);
        auditService.log(actorId, "ROLE_UPDATED", "Updated role " + saved.getName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public RoleResponse updatePermissions(Long id, RolePermissionsRequest request, Long actorId, Set<String> actorRoleNames, HttpServletRequest httpRequest) {
        boolean superAdmin = tenantContext.isSuperAdmin();
        Role role = getOwnedEntity(id, superAdmin);
        rejectIfEditingOwnRole(role, superAdmin, actorRoleNames);
        role.setPermissions(resolvePermissionsWithCeiling(request.getPermissionIds(), superAdmin));
        Role saved = roleRepository.save(role);
        auditService.log(actorId, "ROLE_UPDATED", "Updated permissions for role " + saved.getName(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long actorId, HttpServletRequest httpRequest) {
        Role role = getOwnedEntity(id, tenantContext.isSuperAdmin());
        roleRepository.delete(role);
        auditService.log(actorId, "ROLE_DELETED", "Deleted role " + role.getName(), httpRequest);
    }

    /**
     * Used by EmployeeService when enabling login / assigning a role to an
     * employee's User account - the ONE place role IDs coming from a
     * request are resolved into an actual Role, so the ownership guardrail
     * is impossible to bypass by calling a different code path.
     */
    @Transactional(readOnly = true)
    public Role resolveAssignableRoleForCurrentTenant(Long roleId) {
        if (tenantContext.isSuperAdmin()) {
            return roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
        }
        Long tenantId = tenantContext.requireCurrentTenantId();
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
        boolean ownTenantRole = tenantId.equals(role.getClientCompanyId());
        boolean assignableGlobal = role.getClientCompanyId() == null && TENANT_ASSIGNABLE_GLOBAL_ROLES.contains(role.getName());
        if (!ownTenantRole && !assignableGlobal) {
            throw new TenantAccessDeniedException("Role " + roleId + " is not assignable within the current tenant");
        }
        return role;
    }

    /**
     * Permissions that any ROLE_UPDATE holder may freely grant/revoke on ANY role - including
     * their own - regardless of whether they personally hold that permission themselves. This
     * bypasses the "can't grant what you don't have" ceiling below entirely, in both directions,
     * for these specific permissions. SUPER_ADMIN is unaffected either way - it already bypasses
     * every restriction below.
     */
    /**
     * Blanket rule: nobody except SUPER_ADMIN may edit a role that is currently one of their OWN
     * roles - not the name, not the description, and not a single permission on it, even ones
     * they already hold themselves. This is a stronger, simpler rule than trying to reason about
     * which specific permission changes would or wouldn't be self-escalation; editing your own
     * role at all requires a Super Admin (or another admin who doesn't hold this role) to do it.
     */
    private void rejectIfEditingOwnRole(Role role, boolean superAdmin, Set<String> actorRoleNames) {
        if (!superAdmin && actorRoleNames != null && actorRoleNames.contains(role.getName())) {
            throw new BadRequestException(
                    "You cannot edit a role you yourself currently hold (\"" + role.getName()
                            + "\") - ask a Super Admin, or another admin who doesn't hold this role, to make this change.");
        }
    }

    private static final Set<String> UNRESTRICTED_PERMISSIONS = Set.of("PAYSLIP_SELF_VIEW");

    private Set<Permission> resolvePermissionsWithCeiling(Set<Long> ids, boolean superAdmin) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(ids));
        if (permissions.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more permission IDs do not exist");
        }

        if (!superAdmin) {
            Set<String> ownPermissions = tenantContext.currentPermissionNames();
            Set<String> disallowed = permissions.stream()
                    .map(Permission::getName)
                    .filter(name -> !ownPermissions.contains(name))
                    .filter(name -> !UNRESTRICTED_PERMISSIONS.contains(name))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!disallowed.isEmpty()) {
                throw new BadRequestException(
                        "You cannot grant permissions you do not have: " + String.join(", ", disallowed));
            }
        }
        return permissions;
    }

    /** Read access: SUPER_ADMIN sees anything; a tenant user only their own tenant's roles or the safelist. */
    private Role getVisibleEntity(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        if (tenantContext.isSuperAdmin()) return role;
        Long tenantId = tenantContext.currentTenantIdOrNull();
        boolean ownTenantRole = tenantId != null && tenantId.equals(role.getClientCompanyId());
        boolean assignableGlobal = role.getClientCompanyId() == null && TENANT_ASSIGNABLE_GLOBAL_ROLES.contains(role.getName());
        if (!ownTenantRole && !assignableGlobal) {
            throw new TenantAccessDeniedException("Role " + id + " does not belong to the current tenant");
        }
        return role;
    }

    /** Write access: SUPER_ADMIN can modify anything; a tenant user only roles they OWN (created themselves). */
    private Role getOwnedEntity(Long id, boolean superAdmin) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        if (superAdmin) return role;
        Long tenantId = tenantContext.requireCurrentTenantId();
        if (!tenantId.equals(role.getClientCompanyId())) {
            throw new TenantAccessDeniedException("Role " + id + " does not belong to the current tenant");
        }
        return role;
    }

    private RoleResponse toResponse(Role role) {
        Set<String> permissionNames = role.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet());
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.isActive(),
                permissionNames, role.getClientCompanyId() != null);
    }
}
