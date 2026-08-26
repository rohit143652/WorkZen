package com.example.application.login_module.service;

import com.example.application.config.JwtConfig;
import com.example.application.login_module.security.CustomUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Responsible only for JWT access-token issuance and validation.
 * Claims deliberately exclude anything sensitive (no password, no internal IDs beyond subject).
 */
@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        String secret = jwtConfig.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Set the JWT_SECRET environment variable to a random "
                            + "Base64 string of at least 64 bytes (512 bits) before starting the "
                            + "application, e.g. via: openssl rand -base64 64. "
                            + "See backend/.env.example.");
        }
        // Accept either a raw string or a Base64-encoded secret; Base64 is recommended
        // since it reliably yields enough entropy/bytes for HS512.
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException notBase64) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short (" + keyBytes.length + " bytes). It must decode to at "
                            + "least 32 bytes (256 bits), 64 bytes (512 bits) recommended for HS512. "
                            + "Generate one with: openssl rand -base64 64");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(CustomUserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

        var builder = Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("roles", List.copyOf(principal.getRoleNames()))
                .claim("permissions", List.copyOf(principal.getPermissionNames()));

        if (principal.getClientCompanyId() != null) {
            builder.claim("tenantId", principal.getClientCompanyId());
        }

        return builder
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenExpirySeconds() {
        return jwtConfig.getAccessTokenExpiration() / 1000;
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(expectedUsername) && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
