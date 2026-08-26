package com.example.application.login_module.repository;

import com.example.application.login_module.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    long countByActiveTrueAndLockedFalse();
    long countByLockedTrue();

    /** Tenant-scoped lookups - see EmployeeRepository for the same pattern and rationale. */
    Optional<User> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
    Page<User> findAllByClientCompanyId(Long clientCompanyId, Pageable pageable);
    boolean existsByClientCompanyId(Long clientCompanyId);
    long countByClientCompanyIdAndActiveTrueAndLockedFalse(Long clientCompanyId);
    long countByClientCompanyIdAndLockedTrue(Long clientCompanyId);
}
