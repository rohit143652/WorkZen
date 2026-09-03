package com.example.application.login_module.controller;

import com.example.application.common.response.ApiResponse;
import com.example.application.login_module.dto.*;
import com.example.application.login_module.security.CustomUserPrincipal;
import com.example.application.login_module.service.AuthResult;
import com.example.application.login_module.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints. The raw refresh token is delivered via an
 * HttpOnly, Secure, SameSite cookie (see README "Token Storage Strategy")
 * rather than in the JSON body, to reduce XSS exfiltration risk. The access
 * token is short-lived and kept in memory on the frontend.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.cookie.same-site:Strict}")
    private String sameSite;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with username and password")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                              HttpServletRequest httpRequest,
                                                              HttpServletResponse httpResponse) {
        AuthResult<LoginResponse> result = authService.login(request, httpRequest);
        setRefreshCookie(httpResponse, result.getRawRefreshToken());
        result.getBody().setRefreshToken(result.getRawRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Login successful", result.getBody()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new access token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest body,
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieToken,
            HttpServletResponse httpResponse) {

        String rawToken = cookieToken != null ? cookieToken : (body != null ? body.getRefreshToken() : null);
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Refresh token is required");
        }

        AuthResult<RefreshTokenResponse> result = authService.refresh(rawToken);
        setRefreshCookie(httpResponse, result.getRawRefreshToken());
        result.getBody().setRefreshToken(result.getRawRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", result.getBody()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the refresh token and clear the session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest body,
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieToken,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String rawToken = cookieToken != null ? cookieToken : (body != null ? body.getRefreshToken() : null);
        Long userId = principal != null ? principal.getId() : null;
        authService.logout(rawToken, userId, httpRequest);
        clearRefreshCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the current user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                              @AuthenticationPrincipal CustomUserPrincipal principal,
                                                              HttpServletRequest httpRequest,
                                                              HttpServletResponse httpResponse) {
        authService.changePassword(principal.getId(), request, httpRequest);
        clearRefreshCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.success(
                "Password changed successfully. Please log in again."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user's profile")
    public ResponseEntity<ApiResponse<UserInfoResponse>> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        UserInfoResponse info = new UserInfoResponse(
                principal.getId(), principal.getUsername(), principal.getUser().getEmail(),
                principal.getUser().getFirstName(), principal.getUser().getLastName(),
                principal.getRoleNames().stream().toList(), principal.getPermissionNames().stream().toList(),
                principal.getUser().isMustChangePassword());
        return ResponseEntity.ok(ApiResponse.success("OK", info));
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        String cookie = REFRESH_COOKIE_NAME + "=" + token
                + "; Max-Age=" + (refreshTokenExpirationMs / 1000)
                + "; Path=" + REFRESH_COOKIE_PATH
                + "; HttpOnly"
                + (secureCookie ? "; Secure" : "")
                + "; SameSite=" + sameSite;
        response.addHeader("Set-Cookie", cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        String cookie = REFRESH_COOKIE_NAME + "=; Max-Age=0; Path=" + REFRESH_COOKIE_PATH
                + "; HttpOnly" + (secureCookie ? "; Secure" : "") + "; SameSite=" + sameSite;
        response.addHeader("Set-Cookie", cookie);
    }
}
