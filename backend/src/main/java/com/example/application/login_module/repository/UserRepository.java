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
    /** Global - kept for any genuinely cross-tenant lookup, but NOT what validation should use for the "is this email already taken" check anymore now that email uniqueness is per-company (V118). See existsByEmailAndClientCompanyId below for that. */
    boolean existsByEmail(String email);
    /** The actual check to use for "is this email already taken" validation - matches the per-company uniqueness V118 established. NULL clientCompanyId (house/SUPER_ADMIN users) intentionally finds only other NULL-company rows, consistent with how MySQL's unique index itself treats NULL. */
    boolean existsByEmailAndClientCompanyId(String email, Long clientCompanyId);

    long countByActiveTrueAndLockedFalse();
    long countByLockedTrue();

    /** Tenant-scoped lookups - see EmployeeRepository for the same pattern and rationale. */
    Optional<User> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
    Page<User> findAllByClientCompanyId(Long clientCompanyId, Pageable pageable);
    boolean existsByClientCompanyId(Long clientCompanyId);
    long countByClientCompanyIdAndActiveTrueAndLockedFalse(Long clientCompanyId);
    long countByClientCompanyIdAndLockedTrue(Long clientCompanyId);
}
