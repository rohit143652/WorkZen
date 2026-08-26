package com.example.application.login_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.AccountLockedException;
import com.example.application.common.exception.BadRequestException;
import com.example.application.login_module.dto.*;
import com.example.application.login_module.entity.RefreshToken;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates authentication flows. All authorization decisions (lock
 * status, roles, permissions) are derived from the database - nothing is
 * hardcoded in Java.
 */
@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        RefreshTokenService refreshTokenService,
                        LoginAttemptService loginAttemptService,
                        AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResult<LoginResponse> login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (user != null && user.isLocked()) {
            loginAttemptService.record(request.getUsername(), httpRequest, false);
            auditService.log(user.getId(), "LOGIN_FAILED", "Account is locked", httpRequest);
            throw new AccountLockedException("Account is locked. Please contact an administrator.");
        }

        try {
            CustomUserPrincipal principal = authenticate(request.getUsername(), request.getPassword());
            User authenticatedUser = principal.getUser();

            authenticatedUser.setFailedLoginAttempts(0);
            authenticatedUser.setLastLoginAt(LocalDateTime.now());
            userRepository.save(authenticatedUser);

            loginAttemptService.record(request.getUsername(), httpRequest, true);
            auditService.log(authenticatedUser.getId(), "LOGIN_SUCCESS", "User logged in", httpRequest);

            String accessToken = jwtService.generateAccessToken(principal);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(authenticatedUser);

            LoginResponse response = new LoginResponse(accessToken, jwtService.getAccessTokenExpirySeconds(),
                    toUserInfo(principal));
            response.setMessage("Login successful");

            return new AuthResult<>(response, refreshToken.getToken());

        } catch (BadCredentialsException ex) {
            handleFailedAttempt(user, request.getUsername(), httpRequest);
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    private CustomUserPrincipal authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, password);
        return (CustomUserPrincipal) authenticationManager.authenticate(authRequest).getPrincipal();
    }

    private void handleFailedAttempt(User user, String username, HttpServletRequest httpRequest) {
        loginAttemptService.record(username, httpRequest, false);
        if (user == null) {
            return;
        }
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLocked(true);
            auditService.log(user.getId(), "ACCOUNT_LOCKED",
                    "Account locked after " + attempts + " failed login attempts", httpRequest);
        }
        userRepository.save(user);
        auditService.log(user.getId(), "LOGIN_FAILED", "Invalid credentials", httpRequest);
    }

    @Transactional
    public AuthResult<RefreshTokenResponse> refresh(String rawRefreshToken) {
        RefreshToken validated = refreshTokenService.validateAndGet(rawRefreshToken);
        User user = validated.getUser();

        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        String newAccessToken = jwtService.generateAccessToken(principal);

        // Refresh token rotation: revoke the presented token, issue a new one.
        RefreshToken rotated = refreshTokenService.rotate(validated);

        RefreshTokenResponse response = new RefreshTokenResponse(newAccessToken, jwtService.getAccessTokenExpirySeconds());
        return new AuthResult<>(response, rotated.getToken());
    }

    @Transactional
    public void logout(String rawRefreshToken, Long userId, HttpServletRequest httpRequest) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeToken(rawRefreshToken);
        }
        auditService.log(userId, "LOGOUT", "User logged out", httpRequest);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user);
        auditService.log(userId, "PASSWORD_CHANGED", "Password changed successfully", httpRequest);
    }

    private UserInfoResponse toUserInfo(CustomUserPrincipal principal) {
        User user = principal.getUser();
        return new UserInfoResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(),
                List.copyOf(principal.getRoleNames()), List.copyOf(principal.getPermissionNames()),
                user.isMustChangePassword());
    }
}
