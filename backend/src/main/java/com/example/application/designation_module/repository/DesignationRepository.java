package com.example.application.designation_module.repository;

import com.example.application.designation_module.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignationRepository extends JpaRepository<Designation, Long> {
    List<Designation> findAllByClientCompanyIdOrderByNameAsc(Long clientCompanyId);
    List<Designation> findAllByClientCompanyIdAndStatusOrderByNameAsc(Long clientCompanyId, String status);
    Optional<Designation> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
    boolean existsByClientCompanyIdAndNameIgnoreCase(Long clientCompanyId, String name);

    /** Used to look up a designation's base pay when computing an employee's effective salary. */
    Optional<Designation> findByClientCompanyIdAndNameIgnoreCase(Long clientCompanyId, String name);
}
