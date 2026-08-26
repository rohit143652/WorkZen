package com.example.application.salary_structure_module.repository;

import com.example.application.salary_structure_module.entity.SalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, Long> {
    List<SalaryComponent> findAllByClientCompanyIdOrderByDisplayOrderAsc(Long clientCompanyId);
    List<SalaryComponent> findAllByClientCompanyIdAndActiveOrderByDisplayOrderAsc(Long clientCompanyId, boolean active);
    Optional<SalaryComponent> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
    boolean existsByClientCompanyIdAndComponentCodeIgnoreCase(Long clientCompanyId, String componentCode);

    /** Used by CodeGeneratorService to derive the next component code, scoped per tenant. */
    Optional<SalaryComponent> findTopByClientCompanyIdAndComponentCodeStartingWithOrderByComponentCodeDesc(Long clientCompanyId, String prefix);
}
