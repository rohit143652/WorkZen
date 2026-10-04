package com.example.application.payroll_module.repository;

import com.example.application.payroll_module.entity.ProfessionalTaxSlab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionalTaxSlabRepository extends JpaRepository<ProfessionalTaxSlab, Long> {

    List<ProfessionalTaxSlab> findAllByClientCompanyIdOrderByEffectiveFromDescMinSalaryAsc(Long clientCompanyId);

    Optional<ProfessionalTaxSlab> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    /** All ACTIVE slabs covering a given date - used by PayrollProfessionalTaxResolver to find the one whose min/max salary range contains the gross being evaluated. */
    List<ProfessionalTaxSlab> findAllByClientCompanyIdAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNull(
            Long clientCompanyId, String status, java.time.LocalDate asOfDate);

    List<ProfessionalTaxSlab> findAllByClientCompanyIdAndStatusAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
            Long clientCompanyId, String status, java.time.LocalDate asOfDateForFrom, java.time.LocalDate asOfDateForTo);
}
