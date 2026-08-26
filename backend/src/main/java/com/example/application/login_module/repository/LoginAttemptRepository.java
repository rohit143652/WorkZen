package com.example.application.login_module.repository;

import com.example.application.login_module.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
}
