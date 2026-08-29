package com.example.application.employee_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.common.util.SecurePasswordGenerator;
import com.example.application.department_module.service.DepartmentService;
import com.example.application.designation_module.service.DesignationService;
import com.example.application.employee_module.dto.*;
import com.example.application.employee_module.entity.Employee;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.employee_module.repository.EmployeeSpecifications;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.login_module.service.RefreshTokenService;
import com.example.application.role_module.entity.Role;
import com.example.application.role_module.service.RoleService;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.salary_structure_module.dto.AssignSalaryStructureRequest;
import com.example.application.salary_structure_module.dto.EmployeeSalaryStructureResponse;
import com.example.application.salary_structure_module.service.EmployeeSalaryStructureService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Employee Management is the single entry point for employee creation, with
 * an OPTIONAL login account created atomically alongside it. There is no
 * separate "user registration" workflow - see AuthService/UserService for
 * the pre-existing, unmodified authentication core that this module reuses.
 */
@Service
public class EmployeeService {

    private static final String EMPLOYEE_CODE_PREFIX = "EMP";

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final TenantContextService tenantContextService;
    private final DepartmentService departmentService;
    private final DesignationService designationService;
    private final EmployeeSalaryStructureService employeeSalaryStructureService;

    public EmployeeService(EmployeeRepository employeeRepository, UserRepository userRepository,
                            RoleService roleService, PasswordEncoder passwordEncoder,
                            RefreshTokenService refreshTokenService, AuditService auditService,
                            TenantContextService tenantContextService, DepartmentService departmentService,
                            DesignationService designationService,
                            EmployeeSalaryStructureService employeeSalaryStructureService) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.tenantContextService = tenantContextService;
        this.departmentService = departmentService;
        this.designationService = designationService;
        this.employeeSalaryStructureService = employeeSalaryStructureService;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> search(String search, String status, String department,
                                          Boolean loginEnabled, Long clientCompanyIdFilter, Pageable pageable) {
        // A caller with their own tenant (CLIENT_ADMIN/CLIENT_USER) is ALWAYS locked to it,
        // regardless of any clientCompanyIdFilter they send - that value is only honored for
        // callers with no tenant of their own (SUPER_ADMIN, and the pre-existing internal
        // ADMIN role, which keeps its original unrestricted visibility for backward compatibility).
        Long currentTenantId = tenantContextService.currentTenantIdOrNull();
        Long effectiveTenantFilter = currentTenantId != null ? currentTenantId : clientCompanyIdFilter;

        Specification<Employee> spec = Specification
                .where(EmployeeSpecifications.search(search))
                .and(EmployeeSpecifications.hasStatus(status))
                .and(EmployeeSpecifications.hasDepartment(department))
                .and(EmployeeSpecifications.loginEnabled(loginEnabled))
                .and(EmployeeSpecifications.belongsToCompany(effectiveTenantFilter));
        return employeeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    /** Read-only preview of the code create() would auto-assign right now - lets the Add form show/disable it upfront rather than after saving. */
    @Transactional(readOnly = true)
    public String previewNextCode() {
        Long tenantId = tenantContextService.currentTenantIdOrNull();
        String lastCode = employeeRepository
                .findTopByClientCompanyIdAndEmployeeCodeStartingWithOrderByEmployeeCodeDesc(tenantId, EMPLOYEE_CODE_PREFIX)
                .map(Employee::getEmployeeCode)
                .orElse(null);
        return com.example.application.common.util.CodeGeneratorService.nextCode(EMPLOYEE_CODE_PREFIX, lastCode, 4);
    }

    /**
     * Creates the Employee, and - only if enableLogin is true - the linked User
     * and its role assignment, all in one transaction. If any step fails
     * (duplicate username, invalid role, etc.) everything rolls back, including
     * the employee row.
     */
    @Transactional
    public EmployeeResponse create(EmployeeRequest request, Long actorId, HttpServletRequest httpRequest) {
        // The employee's tenant is ALWAYS derived from the creator's own account, never from
        // the request body - there is no clientCompanyId field on EmployeeRequest at all.
        Long tenantId = tenantContextService.currentTenantIdOrNull();

        String employeeCode = request.getEmployeeCode();
        if (employeeCode == null || employeeCode.isBlank()) {
            String lastCode = employeeRepository
                    .findTopByClientCompanyIdAndEmployeeCodeStartingWithOrderByEmployeeCodeDesc(tenantId, EMPLOYEE_CODE_PREFIX)
                    .map(Employee::getEmployeeCode)
                    .orElse(null);
            employeeCode = com.example.application.common.util.CodeGeneratorService.nextCode(EMPLOYEE_CODE_PREFIX, lastCode, 4);
        } else if (employeeRepository.existsByClientCompanyIdAndEmployeeCode(tenantId, employeeCode)) {
            throw new DuplicateResourceException("Employee code already exists: " + employeeCode);
        }
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        validateDepartmentAndDesignation(request.getDepartment(), request.getDesignation());
        if (request.getSalaryStructureId() != null) {
            enforceSalaryUpdatePermission();
        }

        Employee employee = new Employee();
        employee.setClientCompanyId(tenantId);
        applyBasicFields(employee, employeeCode, request.getFirstName(), request.getMiddleName(),
                request.getLastName(), request.getEmail(), request.getMobileNumber(), request.getAlternateMobileNumber(),
                request.getDateOfBirth(), request.getGender(), request.getJoiningDate(), request.getDepartment(),
                request.getDesignation(), request.getEmploymentType(), request.getAddress(), request.getCity(),
                request.getState(), request.getCountry(), request.getPincode());
        employee.setStatus("ACTIVE");
        if (request.getPfApplicable() != null) employee.setPfApplicable(request.getPfApplicable());
        if (request.getEsiApplicable() != null) employee.setEsiApplicable(request.getEsiApplicable());
        if (request.getPtApplicable() != null) employee.setPtApplicable(request.getPtApplicable());

        if (request.isEnableLogin()) {
            if (request.getLoginAccess() == null) {
                throw new BadRequestException("Login access details are required when Enable Login is ON");
            }
            EmployeeLoginAccessRequest login = request.getLoginAccess();
            if (!login.getPassword().equals(login.getConfirmPassword())) {
                throw new BadRequestException("Password and confirm password do not match");
            }
            User user = createLoginUser(login.getUsername(), request.getEmail(), request.getFirstName(),
                    request.getLastName(), login.getPassword(), login.getRoleId(), tenantId);
            employee.setUser(user);
        }

        Employee saved = employeeRepository.save(employee);
        auditService.log(actorId, "EMPLOYEE_CREATED", "Created employee " + saved.getEmployeeCode(), httpRequest);
        if (saved.hasLogin()) {
            auditService.log(actorId, "LOGIN_ENABLED",
                    "Login account created for employee " + saved.getEmployeeCode(), httpRequest);
        }

        if (request.getSalaryStructureId() != null) {
            AssignSalaryStructureRequest assignRequest = new AssignSalaryStructureRequest();
            assignRequest.setSalaryStructureId(request.getSalaryStructureId());
            assignRequest.setEffectiveFrom(
                    request.getSalaryEffectiveFrom() != null ? request.getSalaryEffectiveFrom() : request.getJoiningDate());
            employeeSalaryStructureService.assign(saved.getId(), assignRequest, actorId, httpRequest);
        }

        return toResponse(saved);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request, Long actorId, HttpServletRequest httpRequest) {
        Employee employee = getEntity(id);
        if (!employee.getEmail().equals(request.getEmail()) && employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        validateDepartmentAndDesignation(request.getDepartment(), request.getDesignation());
        employee.setFirstName(request.getFirstName());
        employee.setMiddleName(request.getMiddleName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setMobileNumber(request.getMobileNumber());
        employee.setAlternateMobileNumber(request.getAlternateMobileNumber());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setGender(request.getGender());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setDepartment(request.getDepartment());
        employee.setDesignation(request.getDesignation());
        employee.setEmploymentType(request.getEmploymentType());
        employee.setAddress(request.getAddress());
        employee.setCity(request.getCity());
        employee.setState(request.getState());
        employee.setCountry(request.getCountry());
        employee.setPincode(request.getPincode());
        if (request.getPfApplicable() != null) employee.setPfApplicable(request.getPfApplicable());
        if (request.getEsiApplicable() != null) employee.setEsiApplicable(request.getEsiApplicable());
        if (request.getPtApplicable() != null) employee.setPtApplicable(request.getPtApplicable());

        Employee saved = employeeRepository.save(employee);
        reconcileSalaryAssignment(saved, request.getSalaryStructureId(), request.getSalaryEffectiveFrom(), actorId, httpRequest);
        auditService.log(actorId, "EMPLOYEE_UPDATED", "Updated employee " + saved.getEmployeeCode(), httpRequest);
        return toResponse(saved);
    }

    /**
     * Reassigns the employee's Salary Structure only if requestedStructureId is
     * both present and different from their current one - a re-submitted edit
     * form that didn't touch the salary section (or one the caller's UI never
     * showed them, since they lack EMPLOYEE_SALARY_READ) should never
     * accidentally spawn a no-op history row. Requires EMPLOYEE_SALARY_UPDATE
     * and a non-null effective date whenever a real change is requested.
     */
    private void reconcileSalaryAssignment(Employee employee, Long requestedStructureId, java.time.LocalDate effectiveFrom,
                                            Long actorId, HttpServletRequest httpRequest) {
        if (requestedStructureId == null) return;

        EmployeeSalaryStructureResponse current = employeeSalaryStructureService.findCurrent(employee.getId());
        boolean isChange = current == null || !requestedStructureId.equals(current.getSalaryStructureId());
        if (!isChange) return;

        enforceSalaryUpdatePermission();
        if (effectiveFrom == null) {
            throw new BadRequestException("An effective date is required when changing an employee's salary structure");
        }
        AssignSalaryStructureRequest assignRequest = new AssignSalaryStructureRequest();
        assignRequest.setSalaryStructureId(requestedStructureId);
        assignRequest.setEffectiveFrom(effectiveFrom);
        employeeSalaryStructureService.assign(employee.getId(), assignRequest, actorId, httpRequest);
    }

    @Transactional
    public EmployeeResponse activate(Long id, Long actorId, HttpServletRequest httpRequest) {
        Employee employee = getEntity(id);
        employee.setStatus("ACTIVE");
        Employee saved = employeeRepository.save(employee);
        auditService.log(actorId, "EMPLOYEE_ACTIVATED", "Activated employee " + saved.getEmployeeCode(), httpRequest);
        return toResponse(saved);
    }

    /**
     * For an employee who left and is now rejoining - reuses the SAME underlying record (so
     * their entire employment history - past salary structures, attendance, advances, exit
     * settlement, everything - stays intact and attached to this one record) rather than
     * creating a brand new Employee row from scratch. The only thing that changes is the
     * employeeCode itself, freshly generated the exact same way a new employee's code would be,
     * since the old code represents a closed employment period that's now over; reusing it would
     * make attendance/payroll history from the two employment periods impossible to tell apart.
     */
    @Transactional
    public EmployeeResponse rejoin(Long id, Long actorId, HttpServletRequest httpRequest) {
        Employee employee = getEntity(id);
        if ("ACTIVE".equals(employee.getStatus())) {
            throw new BadRequestException("This employee is already active.");
        }
        Long tenantId = tenantContextService.currentTenantIdOrNull();
        String oldCode = employee.getEmployeeCode();
        String lastCode = employeeRepository
                .findTopByClientCompanyIdAndEmployeeCodeStartingWithOrderByEmployeeCodeDesc(tenantId, EMPLOYEE_CODE_PREFIX)
                .map(Employee::getEmployeeCode)
                .orElse(null);
        String newCode = com.example.application.common.util.CodeGeneratorService.nextCode(EMPLOYEE_CODE_PREFIX, lastCode, 4);

        employee.setEmployeeCode(newCode);
        employee.setStatus("ACTIVE");
        Employee saved = employeeRepository.save(employee);

        auditService.log(actorId, "EMPLOYEE_REJOINED",
                "Employee " + oldCode + " rejoined the company - reactivated with new code " + newCode, httpRequest);
        return toResponse(saved);
    }

    /** Deactivating an employee also disables (never deletes) any linked login and revokes its refresh tokens. */
    @Transactional
    public EmployeeResponse deactivate(Long id, Long actorId, HttpServletRequest httpRequest) {
        Employee employee = getEntity(id);
        employee.setStatus("INACTIVE");
        if (employee.hasLogin()) {
            User user = employee.getUser();
            user.setActive(false);
            userRepository.save(user);
            refreshTokenService.revokeAllForUser(user);
            auditService.log(actorId, "LOGIN_DISABLED",
                    "Login disabled as a result of deactivating employee " + employee.getEmployeeCode(), httpRequest);
        }
        Employee saved = employeeRepository.save(employee);
        auditService.log(actorId, "EMPLOYEE_DEACTIVATED", "Deactivated employee " + saved.getEmployeeCode(), httpRequest);
        return toResponse(saved);
    }

    /**
     * If the employee already has a User (previously disabled), reactivates it -
     * never creates a duplicate. If not, creates a brand-new login account.
     * Username/password/roleId are optional when reactivating; supplying them
     * lets an admin change credentials at the same time.
     */
    @Transactional
    public EmployeeResponse enableLogin(Long id, EnableLoginRequest request, Long actorId, HttpServletRequest httpRequest) {
        Employee employee = getEntity(id);
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new BadRequestException("Cannot enable login for an inactive employee");
        }

        if (employee.hasLogin()) {
            User user = employee.getUser();
            if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
                if (userRepository.existsByUsername(request.getUsername())) {
                    throw new DuplicateResourceException("Username already taken: " + request.getUsername());
                }
                user.setUsername(request.getUsername());
            }
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user.setPasswordChangedAt(LocalDateTime.now());
            }
            if (request.getRoleId() != null) {
                user.setRoles(new HashSet<>(List.of(resolveRole(request.getRoleId()))));
            }
            user.setActive(true);
            user.setLocked(false);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        } else {
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                throw new BadRequestException("Username is required to enable login");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new BadRequestException("Password is required to enable login");
            }
            if (request.getRoleId() == null) {
                throw new BadRequestException("A role is required to enable login");
            }
            User user = createLoginUser(request.getUsername(), employee.getEmail(), employee.getFirstName(),
                    employee.getLastName(), request.getPassword(), request.getRoleId(), employee.getClientCompanyId());
            employee.setUser(user);
        }

        Employee saved = employeeRepository.save(employee);
        auditService.log(actorId, "LOGIN_ENABLED", "Login enabled for employee " + saved.getEmployeeCode(), httpRequest);
        return toResponse(saved);
    }

    /** Disables the linked login account and revokes its refresh tokens. The employee record is untouched. */
    @Transactional
    public EmployeeResponse disableLogin(Long id, Long actorId, HttpServletRequest httpRequest) {
        Employee employee = getEntity(id);
        if (!employee.hasLogin()) {
            throw new BadRequestException("This employee does not have a login account");
        }
        User user = employee.getUser();
        user.setActive(false);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);

        auditService.log(actorId, "LOGIN_DISABLED", "Login disabled for employee " + employee.getEmployeeCode(), httpRequest);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse assignRole(Long id, AssignRoleRequest request, Long actorId, HttpServletRequest httpRequest) {
        Employee employee = getEntity(id);
        if (!employee.hasLogin()) {
            throw new BadRequestException("This employee does not have a login account");
        }
        User user = employee.getUser();
        user.setRoles(new HashSet<>(List.of(resolveRole(request.getRoleId()))));
        userRepository.save(user);
        auditService.log(actorId, "ROLE_ASSIGNED",
                "Role changed for employee " + employee.getEmployeeCode(), httpRequest);
        return toResponse(employee);
    }

    /** Generates and applies a new temporary password; the caller (controller) returns it exactly once. */
    @Transactional
    public String resetPassword(Long id, Long actorId, HttpServletRequest httpRequest) {
        Employee employee = getEntity(id);
        if (!employee.hasLogin()) {
            throw new BadRequestException("This employee does not have a login account");
        }
        User user = employee.getUser();
        String tempPassword = SecurePasswordGenerator.generate();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);

        auditService.log(actorId, "PASSWORD_RESET",
                "Temporary password issued for employee " + employee.getEmployeeCode(), httpRequest);
        return tempPassword;
    }

    // ---- helpers ----

    /**
     * Enforces that Department/Designation come from this tenant's admin-managed
     * master lists (see DepartmentService/DesignationService) rather than free
     * text, so the Employee form's dropdown values stay authoritative and typos
     * can't silently create near-duplicate departments/designations. For
     * SUPER_ADMIN/house context (no tenant), this is a no-op - see
     * DepartmentService.existsForCurrentTenant.
     */
    private void validateDepartmentAndDesignation(String department, String designation) {
        if (department != null && !department.isBlank() && !departmentService.existsForCurrentTenant(department)) {
            throw new BadRequestException(
                    "Department '" + department + "' is not a valid option. Add it first from Department settings.");
        }
        if (designation != null && !designation.isBlank() && !designationService.existsForCurrentTenant(designation)) {
            throw new BadRequestException(
                    "Designation '" + designation + "' is not a valid option. Add it first from Designation settings.");
        }
    }

    /**
     * EMPLOYEE_SALARY_UPDATE gates actually assigning/reassigning a Salary
     * Structure on an employee - not just viewing one (that's EMPLOYEE_SALARY_READ,
     * enforced separately in toResponse). Reused from the original designation-based
     * pay system's permission of the same name so already-granted roles keep working.
     */
    private void enforceSalaryUpdatePermission() {
        if (!tenantContextService.currentPermissionNames().contains("EMPLOYEE_SALARY_UPDATE")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to set an employee's salary structure");
        }
    }

    private User createLoginUser(String username, String email, String firstName, String lastName,
                                  String rawPassword, Long roleId, Long tenantId) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already taken: " + username);
        }
        // Employees share the users.email uniqueness constraint; an employee's own
        // email is reused for the User row it owns 1:1, so no extra uniqueness check needed here.
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(true);
        user.setMustChangePassword(true);
        // The login account always inherits its tenant from the employee it belongs
        // to - never accepted as separate input, so it can never drift from it.
        user.setClientCompanyId(tenantId);
        user.setRoles(new HashSet<>(List.of(resolveRole(roleId))));
        return userRepository.save(user);
    }

    private Role resolveRole(Long roleId) {
        return roleService.resolveAssignableRoleForCurrentTenant(roleId);
    }

    private void applyBasicFields(Employee e, String code, String firstName, String middleName, String lastName,
                                   String email, String mobile, String altMobile, java.time.LocalDate dob,
                                   String gender, java.time.LocalDate joiningDate, String department,
                                   String designation, String employmentType, String address, String city,
                                   String state, String country, String pincode) {
        e.setEmployeeCode(code);
        e.setFirstName(firstName);
        e.setMiddleName(middleName);
        e.setLastName(lastName);
        e.setEmail(email);
        e.setMobileNumber(mobile);
        e.setAlternateMobileNumber(altMobile);
        e.setDateOfBirth(dob);
        e.setGender(gender);
        e.setJoiningDate(joiningDate);
        e.setDepartment(department);
        e.setDesignation(designation);
        e.setEmploymentType(employmentType);
        e.setAddress(address);
        e.setCity(city);
        e.setState(state);
        e.setCountry(country);
        e.setPincode(pincode);
    }

    private Employee getEntity(Long id) {
        // Tenant-scoped lookup: a CLIENT_ADMIN/CLIENT_USER can NEVER fetch another
        // tenant's employee by guessing/incrementing an ID - findByIdAndClientCompanyId
        // returns empty (surfaced as a generic 404) rather than leaking existence.
        // Callers with no tenant of their own (SUPER_ADMIN, and the pre-existing
        // internal ADMIN role) keep unrestricted lookup, preserving Phase 1 behavior.
        Long tenantId = tenantContextService.currentTenantIdOrNull();
        if (tenantId != null) {
            return employeeRepository.findByIdAndClientCompanyId(id, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        }
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private EmployeeResponse toResponse(Employee e) {
        EmployeeResponse r = new EmployeeResponse();
        r.setId(e.getId());
        r.setEmployeeCode(e.getEmployeeCode());
        r.setFirstName(e.getFirstName());
        r.setMiddleName(e.getMiddleName());
        r.setLastName(e.getLastName());
        r.setEmail(e.getEmail());
        r.setMobileNumber(e.getMobileNumber());
        r.setAlternateMobileNumber(e.getAlternateMobileNumber());
        r.setDateOfBirth(e.getDateOfBirth());
        r.setGender(e.getGender());
        r.setJoiningDate(e.getJoiningDate());
        r.setDepartment(e.getDepartment());
        r.setDesignation(e.getDesignation());
        r.setEmploymentType(e.getEmploymentType());
        r.setAddress(e.getAddress());
        r.setCity(e.getCity());
        r.setState(e.getState());
        r.setCountry(e.getCountry());
        r.setPincode(e.getPincode());
        r.setPfApplicable(e.isPfApplicable());
        r.setEsiApplicable(e.isEsiApplicable());
        r.setPtApplicable(e.isPtApplicable());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());

        boolean canViewSalary = tenantContextService.currentPermissionNames().contains("EMPLOYEE_SALARY_READ");
        r.setSalaryVisible(canViewSalary);
        if (canViewSalary) {
            EmployeeSalaryStructureResponse current = employeeSalaryStructureService.findCurrent(e.getId());
            if (current != null) {
                r.setCurrentSalaryStructureId(current.getSalaryStructureId());
                r.setCurrentSalaryStructureCode(current.getStructureCode());
                r.setCurrentSalaryStructureName(current.getStructureName());
                r.setCurrentSalaryType(current.getSalaryType());
                r.setCurrentSalaryEffectiveFrom(current.getEffectiveFrom());
                r.setCurrentGrossEarnings(current.getGrossEarnings());
            }
        }

        if (e.hasLogin()) {
            User user = e.getUser();
            r.setLoginEnabled(user.isActive());
            r.setUserId(user.getId());
            r.setUsername(user.getUsername());
            r.setUserActive(user.isActive());
            r.setUserLocked(user.isLocked());
            r.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
            r.setLastLoginAt(user.getLastLoginAt());
        } else {
            r.setLoginEnabled(false);
        }
        return r;
    }
}
