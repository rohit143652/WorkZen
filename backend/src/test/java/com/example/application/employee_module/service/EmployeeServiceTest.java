package com.example.application.employee_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.department_module.service.DepartmentService;
import com.example.application.designation_module.service.DesignationService;
import com.example.application.employee_module.dto.EmployeeLoginAccessRequest;
import com.example.application.employee_module.dto.EmployeeRequest;
import com.example.application.employee_module.dto.EnableLoginRequest;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.employee_assignment_module.service.EmployeeAssignmentService;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.login_module.service.RefreshTokenService;
import com.example.application.role_module.entity.Role;
import com.example.application.role_module.service.RoleService;
import com.example.application.salary_structure_module.service.EmployeeSalaryStructureService;
import com.example.application.subscription_module.service.ClientSubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers spec scenarios 1 (Login Required = NO -> no User created), 2 (Login Required = YES ->
 * User created + invitation sent), and 9 (enabling login later reuses the SAME Employee row, no
 * duplicate creation).
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ACTOR_ID = 99L;

    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleService roleService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditService auditService;
    @Mock private TenantContextService tenantContextService;
    @Mock private DepartmentService departmentService;
    @Mock private DesignationService designationService;
    @Mock private EmployeeSalaryStructureService employeeSalaryStructureService;
    @Mock private EmployeeAssignmentService employeeAssignmentService;
    @Mock private ClientSubscriptionService clientSubscriptionService;
    @Mock private EmployeeOnboardingService onboardingService;
    @Mock private EmployeeProfileCompletionService profileCompletionService;

    @InjectMocks
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantContextService.currentTenantIdOrNull()).thenReturn(TENANT_ID);
        lenient().when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            return e;
        });
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(2L);
            return u;
        });
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-hash");
    }

    private EmployeeRequest baseRequest() {
        EmployeeRequest request = new EmployeeRequest();
        request.setFirstName("Neha");
        request.setLastName("Kulkarni");
        request.setEmail("neha@example.com");
        request.setMobileNumber("9876543210");
        request.setJoiningDate(LocalDate.now());
        request.setAadharNumber("123412341234");
        request.setPanNumber("ABCDE1234F");
        return request;
    }

    /** SCENARIO 1: Login Required = NO -> Employee created, absolutely no User row touched. */
    @Test
    void createWithoutLoginCreatesOnlyTheEmployeeRecord() {
        EmployeeRequest request = baseRequest();
        request.setEnableLogin(false);

        service.create(request, ACTOR_ID, null);

        verify(employeeRepository, times(1)).save(any(Employee.class));
        verify(userRepository, never()).save(any(User.class));
        verify(onboardingService, never()).createInvitation(any(), any(), any(), any());
    }

    /** SCENARIO 2: Login Required = YES with no admin-set password -> User created (inactive, random password) and the onboarding invitation is triggered. */
    @Test
    void createWithLoginAndNoPasswordCreatesUserAndSendsInvitation() {
        EmployeeRequest request = baseRequest();
        request.setEnableLogin(true);
        EmployeeLoginAccessRequest login = new EmployeeLoginAccessRequest();
        login.setUsername("neha.kulkarni");
        login.setRoleId(5L);
        request.setLoginAccess(login);

        Role role = new Role();
        role.setId(5L);
        role.setName("SITE_SUPERVISOR");
        when(roleService.resolveAssignableRoleForCurrentTenant(5L)).thenReturn(role);

        service.create(request, ACTOR_ID, null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertFalse(createdUser.isActive(), "account must stay inactive until the employee sets their own password");
        assertEquals("neha.kulkarni", createdUser.getUsername());

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository, atLeastOnce()).save(employeeCaptor.capture());
        assertEquals("NOT_STARTED", employeeCaptor.getValue().getOnboardingStatus());

        verify(onboardingService, times(1)).createInvitation(any(Employee.class), eq(createdUser), eq(ACTOR_ID), isNull());
    }

    /** SCENARIO 2 (admin-set-password variant): providing a password directly skips the invitation entirely - existing behavior preserved. */
    @Test
    void createWithLoginAndAdminSetPasswordSkipsTheInvitation() {
        EmployeeRequest request = baseRequest();
        request.setEnableLogin(true);
        EmployeeLoginAccessRequest login = new EmployeeLoginAccessRequest();
        login.setUsername("neha.kulkarni");
        login.setRoleId(5L);
        login.setPassword("AdminSetPass123");
        login.setConfirmPassword("AdminSetPass123");
        request.setLoginAccess(login);

        Role role = new Role();
        role.setId(5L);
        when(roleService.resolveAssignableRoleForCurrentTenant(5L)).thenReturn(role);

        service.create(request, ACTOR_ID, null);

        verify(onboardingService, never()).createInvitation(any(), any(), any(), any());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isActive(), "an admin-set password should activate the account immediately");
    }

    /** SCENARIO 9: enabling login on a previously no-login employee reuses the SAME Employee row - no second Employee is ever created. */
    @Test
    void enableLoginOnExistingEmployeeReusesTheSameEmployeeNoDuplicateCreated() {
        Employee existing = new Employee();
        existing.setId(42L);
        existing.setClientCompanyId(TENANT_ID);
        existing.setFirstName("Neha");
        existing.setLastName("Kulkarni");
        existing.setEmail("neha@example.com");
        existing.setStatus("ACTIVE");
        when(employeeRepository.findByIdAndClientCompanyId(42L, TENANT_ID)).thenReturn(Optional.of(existing));

        Role role = new Role();
        role.setId(5L);
        when(roleService.resolveAssignableRoleForCurrentTenant(5L)).thenReturn(role);

        EnableLoginRequest request = new EnableLoginRequest();
        request.setUsername("neha.kulkarni");
        request.setRoleId(5L);
        // No password -> self-onboarding path, same as scenario 2.

        service.enableLogin(42L, request, ACTOR_ID, null);

        // save() is called on the EXISTING employee instance, never a freshly-constructed one.
        verify(employeeRepository, atLeastOnce()).save(existing);
        assertEquals(42L, existing.getId(), "the same employee id must be preserved - no duplicate record");
        assertNotNull(existing.getUser(), "the existing employee must now be linked to the new login");
        verify(onboardingService, times(1)).createInvitation(eq(existing), any(User.class), eq(ACTOR_ID), isNull());
    }
}
