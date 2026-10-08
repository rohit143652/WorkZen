package com.example.application.login_module.service;

import com.example.application.audit_module.service.AuditService;
import com.example.application.common.exception.AccountLockedException;
import com.example.application.login_module.dto.LoginRequest;
import com.example.application.login_module.entity.RefreshToken;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.UserRepository;
import com.example.application.login_module.security.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private AuditService auditService;
    @Mock private HttpServletRequest httpServletRequest;
    @Mock private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("super_admin");
        user.setEmail("super_admin@workforce.local");
        user.setPassword("hashed");
        user.setActive(true);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
    }

    @Test
    void login_withValidCredentials_returnsAccessAndRefreshToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("super_admin");
        request.setPassword("admin123");

        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        when(userRepository.findByUsername("super_admin")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("raw-refresh-token");
        refreshToken.setUser(user);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthResult<?> result = authService.login(request, httpServletRequest);

        assertNotNull(result);
        assertEquals("raw-refresh-token", result.getRawRefreshToken());
        verify(loginAttemptService).record("super_admin", httpServletRequest, true);
        verify(auditService).log(eq(1L), eq("LOGIN_SUCCESS"), any(), eq(httpServletRequest));
    }

    @Test
    void login_withInvalidCredentials_recordsFailedAttemptAndThrows() {
        LoginRequest request = new LoginRequest();
        request.setUsername("super_admin");
        request.setPassword("wrong-password");

        when(userRepository.findByUsername("super_admin")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request, httpServletRequest));

        verify(loginAttemptService).record("super_admin", httpServletRequest, false);
        verify(userRepository).save(user);
        assertEquals(1, user.getFailedLoginAttempts());
    }

    @Test
    void login_afterFiveFailedAttempts_locksAccount() {
        user.setFailedLoginAttempts(4);
        LoginRequest request = new LoginRequest();
        request.setUsername("super_admin");
        request.setPassword("wrong-password");

        when(userRepository.findByUsername("super_admin")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request, httpServletRequest));

        assertTrue(user.isLocked());
        verify(auditService).log(eq(1L), eq("ACCOUNT_LOCKED"), any(), eq(httpServletRequest));
    }

    @Test
    void login_whenAccountAlreadyLocked_throwsAccountLockedWithoutAuthenticating() {
        user.setLocked(true);
        LoginRequest request = new LoginRequest();
        request.setUsername("super_admin");
        request.setPassword("admin123");

        when(userRepository.findByUsername("super_admin")).thenReturn(Optional.of(user));

        assertThrows(AccountLockedException.class, () -> authService.login(request, httpServletRequest));
        verifyNoInteractions(authenticationManager);
    }

    // ===================== change password (My Profile) =====================

    private com.example.application.login_module.dto.ChangePasswordRequest changeRequest(String current, String next, String confirm) {
        com.example.application.login_module.dto.ChangePasswordRequest r = new com.example.application.login_module.dto.ChangePasswordRequest();
        r.setCurrentPassword(current);
        r.setNewPassword(next);
        r.setConfirmPassword(confirm);
        return r;
    }

    @org.junit.jupiter.api.Test
    void changingThePasswordWithTheCorrectCurrentOneSavesItClearsTheForcedChangeAndEndsEverySession() {
        user.setMustChangePassword(true);
        org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        org.mockito.Mockito.when(passwordEncoder.matches("oldPass123", "hashed")).thenReturn(true);
        org.mockito.Mockito.when(passwordEncoder.matches("newPass456", "hashed")).thenReturn(false);
        org.mockito.Mockito.when(passwordEncoder.encode("newPass456")).thenReturn("new-hash");

        authService.changePassword(1L, changeRequest("oldPass123", "newPass456", "newPass456"), httpServletRequest);

        org.junit.jupiter.api.Assertions.assertEquals("new-hash", user.getPassword());
        org.junit.jupiter.api.Assertions.assertFalse(user.isMustChangePassword());
        org.mockito.Mockito.verify(refreshTokenService).revokeAllForUser(user);
        org.mockito.Mockito.verify(auditService).log(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("PASSWORD_CHANGED"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @org.junit.jupiter.api.Test
    void aWrongCurrentPasswordIsRejectedAndNothingChanges() {
        org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        org.mockito.Mockito.when(passwordEncoder.matches("wrongOne", "hashed")).thenReturn(false);

        com.example.application.common.exception.BadRequestException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.example.application.common.exception.BadRequestException.class,
                () -> authService.changePassword(1L, changeRequest("wrongOne", "newPass456", "newPass456"), httpServletRequest));

        org.junit.jupiter.api.Assertions.assertEquals("Current password is incorrect", ex.getMessage());
        org.junit.jupiter.api.Assertions.assertEquals("hashed", user.getPassword());
        org.mockito.Mockito.verify(refreshTokenService, org.mockito.Mockito.never()).revokeAllForUser(org.mockito.ArgumentMatchers.any());
    }

    @org.junit.jupiter.api.Test
    void theNewPasswordMustDifferFromTheCurrentOne() {
        org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        org.mockito.Mockito.when(passwordEncoder.matches("samePass123", "hashed")).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(com.example.application.common.exception.BadRequestException.class,
                () -> authService.changePassword(1L, changeRequest("samePass123", "samePass123", "samePass123"), httpServletRequest));
        org.junit.jupiter.api.Assertions.assertEquals("hashed", user.getPassword());
    }

    @org.junit.jupiter.api.Test
    void aConfirmPasswordThatDoesNotMatchIsRejectedByTheServerToo() {
        com.example.application.common.exception.BadRequestException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.example.application.common.exception.BadRequestException.class,
                () -> authService.changePassword(1L, changeRequest("oldPass123", "newPass456", "newPass457"), httpServletRequest));

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("do not match"));
        org.mockito.Mockito.verifyNoInteractions(userRepository);   // rejected before anything is even looked up
    }

    @org.junit.jupiter.api.Test
    void callersThatDoNotSendAConfirmPasswordStillWork() {
        org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        org.mockito.Mockito.when(passwordEncoder.matches("oldPass123", "hashed")).thenReturn(true);
        org.mockito.Mockito.when(passwordEncoder.matches("newPass456", "hashed")).thenReturn(false);
        org.mockito.Mockito.when(passwordEncoder.encode("newPass456")).thenReturn("new-hash");

        authService.changePassword(1L, changeRequest("oldPass123", "newPass456", null), httpServletRequest);

        org.junit.jupiter.api.Assertions.assertEquals("new-hash", user.getPassword());
    }
}
