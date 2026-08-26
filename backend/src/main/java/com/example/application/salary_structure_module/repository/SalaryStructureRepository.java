package com.example.application.salary_structure_module.repository;

import com.example.application.salary_structure_module.entity.SalaryStructure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {
    Page<SalaryStructure> findAllByClientCompanyId(Long clientCompanyId, Pageable pageable);
    Optional<SalaryStructure> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
    boolean existsByClientCompanyIdAndStructureCodeIgnoreCase(Long clientCompanyId, String structureCode);
    java.util.List<SalaryStructure> findAllByClientCompanyIdAndStatusOrderByStructureNameAsc(Long clientCompanyId, String status);

    /** Used by CodeGeneratorService to derive the next structure code, scoped per tenant. */
    Optional<SalaryStructure> findTopByClientCompanyIdAndStructureCodeStartingWithOrderByStructureCodeDesc(Long clientCompanyId, String prefix);
}
