package com.example.application.login_module.dto;

public class RefreshTokenResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;

    /** Same reasoning as LoginResponse.refreshToken - only meaningful to a native app persisting login across restarts; browsers ignore it and keep using the cookie. */
    private String refreshToken;

    public RefreshTokenResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
