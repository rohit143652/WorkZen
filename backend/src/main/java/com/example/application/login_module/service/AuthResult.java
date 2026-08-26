package com.example.application.login_module.service;

/**
 * Simple carrier that pairs a JSON-serializable response body with the raw
 * refresh token, which is never placed on the response DTO itself and instead
 * written by the controller into an HttpOnly cookie.
 */
public class AuthResult<T> {
    private final T body;
    private final String rawRefreshToken;

    public AuthResult(T body, String rawRefreshToken) {
        this.body = body;
        this.rawRefreshToken = rawRefreshToken;
    }

    public T getBody() { return body; }
    public String getRawRefreshToken() { return rawRefreshToken; }
}
