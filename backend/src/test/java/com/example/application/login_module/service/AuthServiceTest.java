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
}
