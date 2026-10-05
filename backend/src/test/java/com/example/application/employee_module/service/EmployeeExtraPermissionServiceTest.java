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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeExtraPermissionServiceTest {

    private static final Long TENANT = 7L;
    private static final Long EMPLOYEE_ID = 10L;
    private static final Long ACTOR_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    @Mock private EmployeeRepository employeeRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private TenantContextService tenantContext;
    @Mock private AuditService auditService;

    private EmployeeExtraPermissionService service;

    private Permission attendance;      // the editor holds it
    private Permission payroll;         // the editor does NOT hold it
    private Permission payslipSelf;     // exempt from the ceiling, like for roles
    private Permission fromRole;        // already given by the employee's role
    private Employee employee;
    private User target;

    private static Permission permission(long id, String name) {
        Permission p = new Permission();
        p.setId(id);
        p.setName(name);
        p.setDescription(name + " description");
        p.setActive(true);
        return p;
    }

    @BeforeEach
    void setUp() {
        service = new EmployeeExtraPermissionService(employeeRepository, permissionRepository, tenantContext, auditService);

        attendance = permission(101, "ATTENDANCE_READ");
        payroll = permission(102, "PAYROLL_REGISTER_EXPORT");
        payslipSelf = permission(103, "PAYSLIP_SELF_VIEW");
        fromRole = permission(104, "EMPLOYEE_READ");

        Role role = new Role();
        role.setId(50L);
        role.setName("HR_ADMIN");
        role.setPermissions(new HashSet<>(Set.of(fromRole)));

        target = new User();
        target.setId(TARGET_USER_ID);
        target.setUsername("asha.patil");
        target.setRoles(new HashSet<>(Set.of(role)));

        employee = new Employee();
        employee.setId(EMPLOYEE_ID);
        employee.setClientCompanyId(TENANT);
        employee.setEmployeeCode("EMP0010");
        employee.setUser(target);

        // A normal Client Admin: holds attendance + employee-read, NOT payroll, and is not a Super Admin.
        lenient().when(tenantContext.isSuperAdmin()).thenReturn(false);
        lenient().when(tenantContext.currentPermissionNames()).thenReturn(Set.of("ATTENDANCE_READ", "EMPLOYEE_READ", "USER_PERMISSION_MANAGE"));
        lenient().when(tenantContext.requireCurrentTenantId()).thenReturn(TENANT);
        lenient().when(employeeRepository.findByIdAndClientCompanyId(EMPLOYEE_ID, TENANT)).thenReturn(Optional.of(employee));
        lenient().when(permissionRepository.findAll()).thenReturn(List.of(attendance, payroll, payslipSelf, fromRole));
    }

    private void idsResolveTo(Permission... permissions) {
        when(permissionRepository.findAllById(any())).thenReturn(List.of(permissions));
    }

    private static ExtraPermissionsRequest ids(Long... ids) {
        return new ExtraPermissionsRequest(List.of(ids));
    }

    private Set<String> extraNames() {
        return target.getExtraPermissions().stream().map(Permission::getName).collect(Collectors.toSet());
    }

    // ---------------- granting ----------------

    @Test
    void anEditorCanGrantAPermissionTheyHoldThemselves() {
        idsResolveTo(attendance);

        service.update(EMPLOYEE_ID, ids(101L), ACTOR_ID, null);

        assertEquals(Set.of("ATTENDANCE_READ"), extraNames());
        verify(auditService).log(eq(ACTOR_ID), eq("USER_EXTRA_PERMISSIONS_UPDATED"), contains("added: ATTENDANCE_READ"), any());
    }

    @Test
    void anEditorCannotGrantAPermissionTheyDoNotHold() {
        idsResolveTo(payroll);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.update(EMPLOYEE_ID, ids(102L), ACTOR_ID, null));

        assertTrue(ex.getMessage().contains("PAYROLL_REGISTER_EXPORT"));
        assertTrue(target.getExtraPermissions().isEmpty());
        verify(auditService, never()).log(any(), any(), any(), any());
    }

    @Test
    void payslipSelfViewIsExemptFromTheCeilingExactlyLikeForRoles() {
        idsResolveTo(payslipSelf);

        service.update(EMPLOYEE_ID, ids(103L), ACTOR_ID, null);

        assertEquals(Set.of("PAYSLIP_SELF_VIEW"), extraNames());
    }

    @Test
    void aSuperAdminMayGrantAnything() {
        when(tenantContext.isSuperAdmin()).thenReturn(true);
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        idsResolveTo(payroll);

        service.update(EMPLOYEE_ID, ids(102L), ACTOR_ID, null);

        assertEquals(Set.of("PAYROLL_REGISTER_EXPORT"), extraNames());
    }

    @Test
    void aPermissionTheRoleAlreadyGivesIsRejectedAsRedundant() {
        idsResolveTo(fromRole);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.update(EMPLOYEE_ID, ids(104L), ACTOR_ID, null));

        assertTrue(ex.getMessage().contains("Already given by this employee's role"));
        assertTrue(target.getExtraPermissions().isEmpty());
    }

    @Test
    void anUnknownPermissionIdIsRejected() {
        when(permissionRepository.findAllById(any())).thenReturn(List.of(attendance));   // asked for two, only one exists
        assertThrows(ResourceNotFoundException.class, () -> service.update(EMPLOYEE_ID, ids(101L, 999L), ACTOR_ID, null));
        assertTrue(target.getExtraPermissions().isEmpty());
    }

    // ---------------- who / whom ----------------

    @Test
    void youCannotChangeYourOwnAdditionalPermissions() {
        assertThrows(BadRequestException.class, () -> service.update(EMPLOYEE_ID, ids(101L), TARGET_USER_ID, null));
        assertTrue(target.getExtraPermissions().isEmpty());
        verify(auditService, never()).log(any(), any(), any(), any());
    }

    @Test
    void anotherCompanysEmployeeIsNotFound() {
        when(employeeRepository.findByIdAndClientCompanyId(EMPLOYEE_ID, TENANT)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.view(EMPLOYEE_ID));
        assertThrows(ResourceNotFoundException.class, () -> service.update(EMPLOYEE_ID, ids(101L), ACTOR_ID, null));
    }

    @Test
    void anEmployeeWithoutALoginHasNothingToGivePermissionsTo() {
        employee.setUser(null);
        assertThrows(BadRequestException.class, () -> service.view(EMPLOYEE_ID));
        assertThrows(BadRequestException.class, () -> service.update(EMPLOYEE_ID, ids(101L), ACTOR_ID, null));
    }

    // ---------------- removing / leaving alone ----------------

    @Test
    void anEditorCanRemoveAnExtraTheyAreAllowedToManage() {
        target.getExtraPermissions().add(attendance);

        // An empty request never reaches the permission lookup, so nothing is stubbed for it.
        service.update(EMPLOYEE_ID, new ExtraPermissionsRequest(List.of()), ACTOR_ID, null);

        assertTrue(target.getExtraPermissions().isEmpty());
        verify(auditService).log(eq(ACTOR_ID), eq("USER_EXTRA_PERMISSIONS_UPDATED"), contains("removed: ATTENDANCE_READ"), any());
    }

    @Test
    void anExtraTheEditorDoesNotHoldSurvivesTheirEditUntouched() {
        // A Super Admin once granted payroll. This Client Admin doesn't hold payroll, so they can neither
        // remove it nor - by omitting it from the request - silently strip it.
        target.getExtraPermissions().add(payroll);
        idsResolveTo(attendance);

        service.update(EMPLOYEE_ID, ids(101L), ACTOR_ID, null);

        assertEquals(Set.of("ATTENDANCE_READ", "PAYROLL_REGISTER_EXPORT"), extraNames());
    }

    @Test
    void savingExactlyWhatIsAlreadyThereChangesNothingAndLogsNothing() {
        target.getExtraPermissions().add(attendance);
        idsResolveTo(attendance);

        service.update(EMPLOYEE_ID, ids(101L), ACTOR_ID, null);

        assertEquals(Set.of("ATTENDANCE_READ"), extraNames());
        verify(auditService, never()).log(any(), any(), any(), any());
    }

    // ---------------- what the editor is offered ----------------

    @Test
    void theEditorOffersOnlyWhatTheEditorMayGrantAndFlagsLockedExtras() {
        target.getExtraPermissions().add(payroll);   // granted earlier by someone with more authority

        ExtraPermissionsResponse view = service.view(EMPLOYEE_ID);

        List<String> offered = view.grantable().stream().map(PermissionOption::name).toList();
        assertTrue(offered.contains("ATTENDANCE_READ"));
        assertTrue(offered.contains("PAYSLIP_SELF_VIEW"), "exempt permissions are always offered");
        assertFalse(offered.contains("EMPLOYEE_READ"), "already given by the role - never offered");

        PermissionOption payrollOption = view.grantable().stream().filter(o -> o.name().equals("PAYROLL_REGISTER_EXPORT")).findFirst().orElseThrow();
        assertTrue(payrollOption.locked(), "currently granted but not the editor's to change");
        PermissionOption attendanceOption = view.grantable().stream().filter(o -> o.name().equals("ATTENDANCE_READ")).findFirst().orElseThrow();
        assertFalse(attendanceOption.locked());

        assertEquals(List.of("EMPLOYEE_READ"), view.rolePermissions().stream().map(PermissionOption::name).toList());
        assertEquals(List.of("HR_ADMIN"), view.roleNames());
        assertEquals("asha.patil", view.username());
    }

    @Test
    void aPermissionTheEditorLacksIsNotOfferedAtAllUnlessAlreadyGranted() {
        ExtraPermissionsResponse view = service.view(EMPLOYEE_ID);
        assertFalse(view.grantable().stream().anyMatch(o -> o.name().equals("PAYROLL_REGISTER_EXPORT")));
    }
}
