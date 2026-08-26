package com.example.application.login_module.service;

import com.example.application.login_module.entity.LoginAttempt;
import com.example.application.login_module.repository.LoginAttemptRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

    public LoginAttemptService(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Transactional
    public void record(String username, HttpServletRequest request, boolean success) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(resolveClientIp(request));
        attempt.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        attempt.setSuccess(success);
        loginAttemptRepository.save(attempt);
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
