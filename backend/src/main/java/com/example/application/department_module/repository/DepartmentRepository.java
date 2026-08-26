package com.example.application.department_module.repository;

import com.example.application.department_module.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByClientCompanyIdOrderByNameAsc(Long clientCompanyId);
    List<Department> findAllByClientCompanyIdAndStatusOrderByNameAsc(Long clientCompanyId, String status);
    Optional<Department> findByIdAndClientCompanyId(Long id, Long clientCompanyId);
    boolean existsByClientCompanyIdAndNameIgnoreCase(Long clientCompanyId, String name);
}
