package com.example.application.payroll_module.service;

import com.example.application.common.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * The ONE place that answers "can this action happen from this PayrollRun
 * status?" (architecture refactor Phase 7, spec section 36 - "do not
 * scatter status checks throughout multiple controllers"). PayrollRunService
 * delegates every status check to this class instead of repeating inline
 * conditionals; the actual state transitions still happen in
 * PayrollRunService, this class only validates whether one is currently
 * allowed.
 *
 * Allowed transitions (spec section 2):
 *   DRAFT      -> CALCULATED   (calculate)
 *   CALCULATED -> CALCULATED   (recalculate)
 *   CALCULATED -> APPROVED     (approve)
 *   APPROVED   -> PAID         (mark paid)
 *   DRAFT      -> CANCELLED    (cancel)
 *   CALCULATED -> CANCELLED    (cancel)
 *   APPROVED   -> CALCULATED   (reopen - authorized + reason required)
 *
 * Never allowed, by design, through this normal workflow:
 *   PAID -> anything (a future Payroll Adjustment/Reversal feature would
 *     handle corrections after payment - not built in this phase)
 *   CANCELLED -> anything (create a new run for the month instead)
 */
@Service
public class PayrollStatusTransitionService {

    private static final Set<String> RECALCULABLE_STATUSES = Set.of("DRAFT", "CALCULATED");
    private static final Set<String> CANCELLABLE_STATUSES = Set.of("DRAFT", "CALCULATED");

    /** Applies to both the first calculation (DRAFT) and every recalculation (CALCULATED). */
    public void assertCalculable(String status) {
        if (!RECALCULABLE_STATUSES.contains(status)) {
            throw new BadRequestException("Payroll run is " + status + " - only DRAFT or CALCULATED runs can be (re)calculated");
        }
    }

    /** Also covers editing a per-employee manual adjustment - same editability boundary as recalculation. */
    public void assertEditable(String status) {
        if (!RECALCULABLE_STATUSES.contains(status)) {
            throw new BadRequestException("Payroll run is " + status + " - it can only be edited while DRAFT or CALCULATED");
        }
    }

    public void assertApprovable(String status) {
        if (!"CALCULATED".equals(status)) {
            throw new BadRequestException("Payroll run must be CALCULATED before it can be approved (current status: " + status + ")");
        }
    }

    public void assertPayable(String status) {
        if (!"APPROVED".equals(status)) {
            throw new BadRequestException("Payroll run must be APPROVED before it can be marked as paid (current status: " + status + ")");
        }
    }

    public void assertCancellable(String status) {
        if (!CANCELLABLE_STATUSES.contains(status)) {
            throw new BadRequestException(
                    "Payroll run is " + status + " - only a DRAFT or CALCULATED run can be cancelled through this action "
                            + "(a PAID run is immutable; an APPROVED run must be reopened first)");
        }
    }

    /** PAID gets its own distinct, explicit message (spec section 12) - it is never a normal validation failure, it is a hard architectural boundary. */
    public void assertReopenable(String status) {
        if ("PAID".equals(status)) {
            throw new BadRequestException("Paid payroll cannot be reopened through the standard workflow.");
        }
        if (!"APPROVED".equals(status)) {
            throw new BadRequestException("Only an APPROVED payroll run can be reopened (current status: " + status + ")");
        }
    }
}
