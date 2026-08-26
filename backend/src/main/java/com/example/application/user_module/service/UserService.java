package com.example.application.user_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.DuplicateResourceException;
import com.example.application.common.exception.ResourceNotFoundException;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.login_module.service.RefreshTokenService;
import com.example.application.role_module.entity.Role;
import com.example.application.role_module.repository.RoleRepository;
import com.example.application.common.tenant.TenantContextService;
import com.example.application.user_module.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final TenantContextService tenantContextService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                        RefreshTokenService refreshTokenService, AuditService auditService,
                        TenantContextService tenantContextService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.tenantContextService = tenantContextService;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        Long tenantId = tenantContextService.currentTenantIdOrNull();
        if (tenantId != null) {
            return userRepository.findAllByClientCompanyId(tenantId, pageable).map(this::toResponse);
        }
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request, Long actorId, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        // Inherited from the creator's own tenant, same rule as employee-linked logins -
        // never accepted as separate input.
        user.setClientCompanyId(tenantContextService.currentTenantIdOrNull());
        user.setRoles(resolveRoles(request.getRoleIds()));

        User saved = userRepository.save(user);
        auditService.log(actorId, "USER_CREATED", "Created user " + saved.getUsername(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request, Long actorId, HttpServletRequest httpRequest) {
        User user = getEntity(id);
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        User saved = userRepository.save(user);
        auditService.log(actorId, "USER_UPDATED", "Updated user " + saved.getUsername(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse updateRoles(Long id, UserRolesRequest request, Long actorId, HttpServletRequest httpRequest) {
        User user = getEntity(id);
        user.setRoles(resolveRoles(request.getRoleIds()));
        User saved = userRepository.save(user);
        auditService.log(actorId, "USER_UPDATED", "Updated roles for user " + saved.getUsername(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long actorId, HttpServletRequest httpRequest) {
        User user = getEntity(id);
        userRepository.delete(user);
        auditService.log(actorId, "USER_DELETED", "Deleted user " + user.getUsername(), httpRequest);
    }

    @Transactional
    public UserResponse activate(Long id, Long actorId, HttpServletRequest httpRequest) {
        User user = getEntity(id);
        user.setActive(true);
        User saved = userRepository.save(user);
        auditService.log(actorId, "USER_ACTIVATED", "Activated user " + saved.getUsername(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse deactivate(Long id, Long actorId, HttpServletRequest httpRequest) {
        User user = getEntity(id);
        user.setActive(false);
        User saved = userRepository.save(user);
        refreshTokenService.revokeAllForUser(saved);
        auditService.log(actorId, "USER_DEACTIVATED", "Deactivated user " + saved.getUsername(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse unlock(Long id, Long actorId, HttpServletRequest httpRequest) {
        User user = getEntity(id);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        User saved = userRepository.save(user);
        auditService.log(actorId, "ACCOUNT_UNLOCKED", "Unlocked user " + saved.getUsername(), httpRequest);
        return toResponse(saved);
    }

    /** Issues a new admin-generated temporary password; the caller returns it exactly once. */
    @Transactional
    public String resetPassword(Long id, Long actorId, HttpServletRequest httpRequest) {
        User user = getEntity(id);
        String tempPassword = com.example.application.common.util.SecurePasswordGenerator.generate();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
        auditService.log(actorId, "PASSWORD_RESET", "Temporary password issued for user " + user.getUsername(), httpRequest);
        return tempPassword;
    }

    /**
     * Lets an admin (e.g. CLIENT_ADMIN, via USER_UPDATE - the same
     * permission resetPassword uses) choose a specific new password for a
     * user, rather than receiving a randomly-generated one. Still forces
     * mustChangePassword and revokes existing sessions, same as
     * resetPassword, since a password an admin just typed is not something
     * only the user knows yet.
     */
    public void setPassword(Long id, AdminSetPasswordRequest request, Long actorId, HttpServletRequest httpRequest) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new com.example.application.common.exception.BadRequestException("New password and confirmation do not match");
        }
        User user = getEntity(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
        auditService.log(actorId, "PASSWORD_SET_BY_ADMIN", "Password set by admin for user " + user.getUsername(), httpRequest);
    }

    private Set<Role> resolveRoles(Set<Long> ids) {
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(ids));
        if (roles.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more role IDs do not exist");
        }
        return roles;
    }

    private User getEntity(Long id) {
        Long tenantId = tenantContextService.currentTenantIdOrNull();
        if (tenantId != null) {
            return userRepository.findByIdAndClientCompanyId(id, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserResponse toResponse(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.isActive(), user.isLocked(), user.getLastLoginAt(), roleNames);
    }
}
