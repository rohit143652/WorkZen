package com.example.application.login_module.dto;

public class RefreshTokenResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;

    public RefreshTokenResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
}
