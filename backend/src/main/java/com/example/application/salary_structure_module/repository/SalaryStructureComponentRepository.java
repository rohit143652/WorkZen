package com.example.application.salary_structure_module.repository;

import com.example.application.salary_structure_module.entity.SalaryStructureComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryStructureComponentRepository extends JpaRepository<SalaryStructureComponent, Long> {
    List<SalaryStructureComponent> findAllBySalaryStructureIdOrderByDisplayOrderAsc(Long salaryStructureId);
    void deleteAllBySalaryStructureId(Long salaryStructureId);
    boolean existsBySalaryComponentId(Long salaryComponentId);
}
