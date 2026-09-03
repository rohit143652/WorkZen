package com.example.application.login_module.dto;

public class LoginResponse {
    private boolean success = true;
    private String message = "Login successful";
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserInfoResponse user;

    /**
     * Optional, set by AuthController from the same raw token it also puts in the HttpOnly
     * cookie. Browsers should keep ignoring this and rely on the cookie exactly as before - it
     * exists ONLY so a packaged native app (Capacitor/APK) has a way to persist login across a
     * full app restart, where a cross-origin HttpOnly cookie set over plain HTTP is unreliable
     * (SameSite=None requires HTTPS, which this deployment doesn't have). The native app stores
     * this in Capacitor's Preferences (not exposed to any web page's JS) and sends it back
     * explicitly on /api/auth/refresh, which already accepted a body-supplied token as a
     * fallback before this field existed.
     */
    private String refreshToken;

    public LoginResponse() {}

    public LoginResponse(String accessToken, long expiresIn, UserInfoResponse user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    public UserInfoResponse getUser() { return user; }
    public void setUser(UserInfoResponse user) { this.user = user; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
