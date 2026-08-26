package com.example.application.advance_module.repository;

import com.example.application.advance_module.entity.AdvanceRecoveryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdvanceRecoveryTransactionRepository extends JpaRepository<AdvanceRecoveryTransaction, Long> {
    /** Source-aware lookup (architecture refactor Phase 5) - PAYROLL and MANUAL_SETTLEMENT rows for the same advance+month are now separate, co-existing rows (see V70/V71), so the upsert key must include source too. */
    Optional<AdvanceRecoveryTransaction> findByAdvanceIdAndYearAndMonthAndSource(Long advanceId, int year, int month, String source);

    /** Every recovery ever applied against one advance - what getOutstanding() sums to compute how much has already been recovered. */
    List<AdvanceRecoveryTransaction> findAllByAdvanceId(Long advanceId);

    /** Batch equivalent of findAllByAdvanceId() for many advances at once - avoids an N+1 query when summing recovery across a whole tenant's (or employee's) advance list, e.g. the Advance Dashboard summary. */
    List<AdvanceRecoveryTransaction> findAllByAdvanceIdIn(List<Long> advanceIds);

    /** Same rows, newest first - for the advance's own recovery-history view (spec section 22/24). */
    List<AdvanceRecoveryTransaction> findAllByAdvanceIdOrderByYearDescMonthDescCreatedAtDesc(Long advanceId);

    /** Full recovery history for an employee, most recent first - for the Advances tab's history table. */
    List<AdvanceRecoveryTransaction> findAllByClientCompanyIdAndEmployeeIdOrderByYearDescMonthDesc(Long clientCompanyId, Long employeeId);
}
