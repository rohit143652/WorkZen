package com.example.application.role_module.repository;

import com.example.application.role_module.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);

    /** Specifically the GLOBAL/house role with this name, ignoring any tenant-scoped role that happens to share it. */
    Optional<Role> findByClientCompanyIdIsNullAndName(String name);

    /** All roles belonging to a specific tenant (a Client Admin's own custom roles). */
    List<Role> findAllByClientCompanyId(Long clientCompanyId);

    /** Global/house roles that are safe for a tenant to assign to their own employees by default. */
    List<Role> findAllByClientCompanyIdIsNullAndNameIn(Collection<String> names);

    Optional<Role> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
    boolean existsByClientCompanyIdAndNameIgnoreCase(Long clientCompanyId, String name);
    boolean existsByClientCompanyIdIsNullAndNameIgnoreCase(String name);
}
