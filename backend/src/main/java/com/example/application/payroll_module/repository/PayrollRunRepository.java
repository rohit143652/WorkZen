package com.example.application.payroll_module.repository;

import com.example.application.payroll_module.entity.PayrollRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    Optional<PayrollRun> findByIdAndClientCompanyId(Long id, Long clientCompanyId);

    /** Duplicate-run guard - see PayrollRunService.createRun(). CANCELLED runs are excluded so a month can be reprocessed after cancellation. */
    List<PayrollRun> findAllByClientCompanyIdAndYearAndMonthAndStatusNot(Long clientCompanyId, int year, int month, String excludedStatus);

    Page<PayrollRun> findAllByClientCompanyId(Long clientCompanyId, Pageable pageable);

    Page<PayrollRun> findAllByClientCompanyIdAndYear(Long clientCompanyId, int year, Pageable pageable);

    Page<PayrollRun> findAllByClientCompanyIdAndYearAndMonth(Long clientCompanyId, int year, int month, Pageable pageable);

    Page<PayrollRun> findAllByClientCompanyIdAndStatus(Long clientCompanyId, String status, Pageable pageable);

    Page<PayrollRun> findAllByClientCompanyIdAndYearAndStatus(Long clientCompanyId, int year, String status, Pageable pageable);

    Page<PayrollRun> findAllByClientCompanyIdAndYearAndMonthAndStatus(Long clientCompanyId, int year, int month, String status, Pageable pageable);
}
