package com.example.application.client_company_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.client_company_module.dto.ClientAdminLoginRequest;
import com.example.application.client_company_module.dto.ClientCompanyRequest;
import com.example.application.client_company_module.dto.ClientCompanyResponse;
import com.example.application.client_company_module.entity.ClientCompany;
import com.example.application.client_company_module.repository.ClientCompanyRepository;
import com.example.application.common.exception.BadRequestException;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.employee_module.repository.EmployeeRepository;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.login_module.service.RefreshTokenService;
import com.example.application.role_module.entity.Role;
import com.example.application.role_module.repository.RoleRepository;
import com.example.application.site_module.repository.SiteRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SUPER_ADMIN-only: creates and manages the tenants themselves. This is
 * deliberately the ONE place a clientCompanyId is ever picked rather than
 * derived - because SUPER_ADMIN, by definition, has no tenant of its own
 * and is the only role authorized to create one. Everything else in the
 * system (EmployeeService, SiteService, EmployeeAssignmentService) derives
 * its tenant from TenantContextService and never accepts one as input.
 */
@Service
public class ClientCompanyService {

    private static final String CLIENT_ADMIN_ROLE = "CLIENT_ADMIN";
    private static final String COMPANY_CODE_PREFIX = "CLI";

    private final ClientCompanyRepository clientCompanyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final SiteRepository siteRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final StarterRoleSeederService starterRoleSeederService;

    public ClientCompanyService(ClientCompanyRepository clientCompanyRepository, UserRepository userRepository,
                                 RoleRepository roleRepository, EmployeeRepository employeeRepository,
                                 SiteRepository siteRepository,
                                 PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService,
                                 AuditService auditService, StarterRoleSeederService starterRoleSeederService) {
        this.clientCompanyRepository = clientCompanyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
        this.siteRepository = siteRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.starterRoleSeederService = starterRoleSeederService;
    }

    @Transactional(readOnly = true)
    public Page<ClientCompanyResponse> findAll(Pageable pageable) {
        return clientCompanyRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ClientCompanyResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    /** Read-only preview of the code create() would auto-assign right now - lets the Add form show/disable it upfront rather than after saving. */
    @Transactional(readOnly = true)
    public String previewNextCode() {
        String lastCode = clientCompanyRepository
                .findTopByCompanyCodeStartingWithOrderByCompanyCodeDesc(COMPANY_CODE_PREFIX)
                .map(ClientCompany::getCompanyCode)
                .orElse(null);
        return com.example.application.common.util.CodeGeneratorService.nextCode(COMPANY_CODE_PREFIX, lastCode, 4);
    }

    /**
     * Creates the Client Company, and - only if createClientAdminLogin is true -
     * a CLIENT_ADMIN User atomically alongside it, mirroring the Employee
     * "Enable Login" pattern. If anything fails, the whole operation rolls back.
     */
    @Transactional
    public ClientCompanyResponse create(ClientCompanyRequest request, Long actorId, HttpServletRequest httpRequest) {
        String companyCode = request.getCompanyCode();
        if (companyCode == null || companyCode.isBlank()) {
            String lastCode = clientCompanyRepository
                    .findTopByCompanyCodeStartingWithOrderByCompanyCodeDesc(COMPANY_CODE_PREFIX)
                    .map(ClientCompany::getCompanyCode)
                    .orElse(null);
            companyCode = com.example.application.common.util.CodeGeneratorService.nextCode(COMPANY_CODE_PREFIX, lastCode, 4);
        } else if (clientCompanyRepository.existsByCompanyCode(companyCode)) {
            throw new DuplicateResourceException("Company code already exists: " + companyCode);
        }

        ClientCompany company = new ClientCompany();
        applyFields(company, request);
        company.setCompanyCode(companyCode);
        company.setStatus("ACTIVE");
        company.setCreatedBy(actorId);
        ClientCompany saved = clientCompanyRepository.save(company);

        // A brand new company otherwise starts with NOTHING but the two system-wide roles
        // (CLIENT_ADMIN, CLIENT_USER) - this gives them a sensible, ready-to-use starting set
        // (HR Admin, Site Admin, Accountant, ...) instead of a Client Admin having to hand-build
        // every role and tick every permission before their team can do anything at all. See
        // StarterRoleSeederService for exactly what each one gets, and why - all of it can be
        // renamed, re-permissioned, or deleted afterward like any other custom role.
        starterRoleSeederService.seedStandardRoles(saved.getId());

        if (request.isCreateClientAdminLogin()) {
            if (request.getClientAdminLogin() == null) {
                throw new BadRequestException("Client Admin login details are required when that option is ON");
            }
            createClientAdminUser(saved, request.getClientAdminLogin());
        }

        auditService.log(actorId, "CLIENT_CREATED", "Created client company " + saved.getCompanyCode(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public ClientCompanyResponse update(Long id, ClientCompanyRequest request, Long actorId, HttpServletRequest httpRequest) {
        ClientCompany company = getEntity(id);
        if (!company.getCompanyCode().equals(request.getCompanyCode())
                && clientCompanyRepository.existsByCompanyCode(request.getCompanyCode())) {
            throw new DuplicateResourceException("Company code already exists: " + request.getCompanyCode());
        }
        applyFields(company, request);
        company.setUpdatedBy(actorId);
        ClientCompany saved = clientCompanyRepository.save(company);
        auditService.log(actorId, "CLIENT_UPDATED", "Updated client company " + saved.getCompanyCode(), httpRequest);
        return toResponse(saved);
    }

    /**
     * Deactivating a tenant also disables every login account under it and
     * revokes their refresh tokens in the same transaction - mirroring
     * EmployeeService.deactivate's cascade to its own login. Historical
     * employee/site/assignment records are preserved (soft state only).
     */
    @Transactional
    public ClientCompanyResponse setStatus(Long id, String status, Long actorId, HttpServletRequest httpRequest) {
        ClientCompany company = getEntity(id);
        company.setStatus(status);
        company.setUpdatedBy(actorId);
        ClientCompany saved = clientCompanyRepository.save(company);

        if ("INACTIVE".equals(status)) {
            List<User> tenantUsers = userRepository.findAllByClientCompanyId(id, Pageable.unpaged()).getContent();
            for (User user : tenantUsers) {
                if (user.isActive()) {
                    user.setActive(false);
                    userRepository.save(user);
                    refreshTokenService.revokeAllForUser(user);
                }
            }
        }

        String action = "ACTIVE".equals(status) ? "CLIENT_ACTIVATED" : "CLIENT_DEACTIVATED";
        auditService.log(actorId, action, "Client company " + saved.getCompanyCode() + " status set to " + status, httpRequest);
        return toResponse(saved);
    }

    private void createClientAdminUser(ClientCompany company, ClientAdminLoginRequest loginRequest) {
        if (userRepository.existsByUsername(loginRequest.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + loginRequest.getUsername());
        }
        Role clientAdminRole = roleRepository.findByClientCompanyIdIsNullAndName(CLIENT_ADMIN_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLIENT_ADMIN role not found - has the tenant migration (V14) run?"));

        User user = new User();
        user.setUsername(loginRequest.getUsername());
        user.setEmail(company.getEmail() != null ? company.getEmail() : loginRequest.getUsername() + "@" + company.getCompanyCode() + ".local");
        user.setPassword(passwordEncoder.encode(loginRequest.getPassword()));
        user.setFirstName(company.getCompanyName());
        user.setActive(true);
        user.setMustChangePassword(true);
        // Always the company just created - never accepted as separate input.
        user.setClientCompanyId(company.getId());
        Set<Role> roles = new HashSet<>();
        roles.add(clientAdminRole);
        user.setRoles(roles);
        userRepository.save(user);
    }

    private void applyFields(ClientCompany c, ClientCompanyRequest r) {
        c.setCompanyCode(r.getCompanyCode());
        c.setCompanyName(r.getCompanyName());
        c.setLegalName(r.getLegalName());
        c.setEmail(r.getEmail());
        c.setPhone(r.getPhone());
        c.setAlternatePhone(r.getAlternatePhone());
        c.setAddress(r.getAddress());
        c.setCity(r.getCity());
        c.setState(r.getState());
        c.setCountry(r.getCountry());
        c.setPincode(r.getPincode());
        c.setContactPersonName(r.getContactPersonName());
        c.setContactPersonEmail(r.getContactPersonEmail());
        c.setContactPersonPhone(r.getContactPersonPhone());
    }

    private ClientCompany getEntity(Long id) {
        return clientCompanyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client company not found: " + id));
    }

    private ClientCompanyResponse toResponse(ClientCompany c) {
        ClientCompanyResponse r = new ClientCompanyResponse();
        r.setId(c.getId());
        r.setCompanyCode(c.getCompanyCode());
        r.setCompanyName(c.getCompanyName());
        r.setLegalName(c.getLegalName());
        r.setEmail(c.getEmail());
        r.setPhone(c.getPhone());
        r.setAlternatePhone(c.getAlternatePhone());
        r.setAddress(c.getAddress());
        r.setCity(c.getCity());
        r.setState(c.getState());
        r.setCountry(c.getCountry());
        r.setPincode(c.getPincode());
        r.setContactPersonName(c.getContactPersonName());
        r.setContactPersonEmail(c.getContactPersonEmail());
        r.setContactPersonPhone(c.getContactPersonPhone());
        r.setStatus(c.getStatus());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        r.setTotalEmployees(employeeRepository.countByClientCompanyId(c.getId()));
        r.setTotalSites(siteRepository.countByClientCompanyId(c.getId()));
        r.setHasClientAdminLogin(userRepository.existsByClientCompanyId(c.getId()));
        return r;
    }
}
