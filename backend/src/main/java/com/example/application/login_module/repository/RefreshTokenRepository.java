package com.example.application.login_module.repository;

import com.example.application.login_module.entity.RefreshToken;
import com.example.application.login_module.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user = :user and r.revoked = false")
    void revokeAllByUser(User user);
}
