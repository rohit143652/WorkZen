package com.example.application.login_module.service;

import com.example.application.common.exception.InvalidTokenException;
import com.example.application.config.JwtConfig;
import com.example.application.login_module.entity.RefreshToken;
import com.example.application.login_module.entity.User;
import com.example.application.login_module.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtConfig jwtConfig) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtConfig = jwtConfig;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtConfig.getRefreshTokenExpiration() / 1000));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateAndGet(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }
        User user = refreshToken.getUser();
        if (!user.isActive()) {
            throw new InvalidTokenException("User account is inactive");
        }
        if (user.isLocked()) {
            throw new InvalidTokenException("User account is locked");
        }
        return refreshToken;
    }

    /** Rotation: revoke the presented token and issue a brand new one. */
    @Transactional
    public RefreshToken rotate(RefreshToken oldToken) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);
        return createRefreshToken(oldToken.getUser());
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
}
