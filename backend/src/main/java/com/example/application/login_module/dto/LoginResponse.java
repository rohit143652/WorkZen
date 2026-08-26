package com.example.application.login_module.dto;

public class LoginResponse {
    private boolean success = true;
    private String message = "Login successful";
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserInfoResponse user;

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
}
