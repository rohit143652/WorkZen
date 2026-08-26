package com.example.application.payroll_module.service;

import com.example.application.advance_module.service.EmployeeAdvanceService;
import com.example.application.payroll_module.dto.PayrollCalculationInput;
import com.example.application.payroll_module.dto.PayrollCalculationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * THE SINGLE SOURCE OF TRUTH for monthly Net Pay (architecture refactor
 * Phase 6 - built up incrementally since Phase 1). No other class in the
 * codebase is allowed to compute PF/ESI/PT/Advance Recovery/Net Pay -
 * every other module (Attendance, Leave, Salary Structure, Advance) only
 * ever provides an INPUT to this one calculation, never a competing
 * calculation of its own. See PayrollCalculationInput's own class comment
 * for exactly how each input is expected to already be resolved (tenant
 * policy combined with employee-level applicability) before it reaches
 * here - this class does not re-derive eligibility, it only applies it.
 *
 * MONEY / ROUNDING CONVENTION (spec sections 28-29): every monetary value
 * is a java.math.BigDecimal, scale 2, RoundingMode.HALF_UP, applied
 * consistently at each individual multiplication/division - never a raw
 * float/double anywhere in this class. This is the one rounding
 * convention used for payroll; nothing here invents a second one.
 *
 * CALCULATION SEQUENCE (spec section 5) - each step is its own named
 * private method below, in this exact order:
 *   1. resolveEpfBase()       - Basic+DA if present, else falls back to Gross
 *      (structures with no separate Basic/DA - e.g. DAILY/HOURLY/CONTRACT)
 *   2. calculatePf()          - employee+employer EPF, 0 if not applicable
 *   3. calculateEsi()         - employee+employer ESI, 0 if not applicable
 *                                or Gross exceeds the wage ceiling
 *   4. calculatePt()          - flat Professional Tax, 0 if not applicable
 *   5. calculateAdvanceRecovery() - delegates entirely to
 *      advance_module.EmployeeAdvanceService, capped at whatever room is
 *      left in Gross after PF/ESI/PT/Other Deduction so Net Pay can never
 *      go negative (spec section 19) - any shortfall stays outstanding
 *   6. calculateNetPay()      - Gross - Total Deduct + Allowance
 *
 * Bonus/Overtime/Arrears/Reimbursements (spec sections 4/17) are NOT yet
 * separate inputs - Gross Earnings here is still the single lump figure
 * Salary Structure + attendance proration produces (see
 * payroll_module.PayrollInputResolver). Adding those as genuinely distinct,
 * separately-tracked earnings is a real gap, not an oversight - there is
 * currently no UI/data model capturing a monthly Bonus or Overtime amount
 * per employee anywhere in the project (EmployeePayrollAdjustment only has
 * otherManualDeduction and allowance). Building that input mechanism is
 * new scope beyond "orchestrate existing inputs," so it's deliberately
 * left as documented future work rather than invented here.
 */
@Service
public class PayrollCalculationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final EmployeeAdvanceService advanceService;

    public PayrollCalculationService(EmployeeAdvanceService advanceService) {
        this.advanceService = advanceService;
    }

    /**
     * The one public calculation entry point (spec section 46 - "there should be one public
     * payroll calculation entry point"). Not read-only: calculateAdvanceRecovery() below upserts
     * an AdvanceRecoveryTransaction row as a documented side effect of resolving this month's
     * recovery (same idempotent-upsert pattern already used by the Paid Leave module).
     */
    @Transactional
    public PayrollCalculationResult calculate(PayrollCalculationInput input) {
        BigDecimal basic = nz(input.getBasicSalary());
        BigDecimal da = nz(input.getDa());
        boolean hasGross = input.getTotalGross() != null;
        BigDecimal totalGross = nz(input.getTotalGross());

        BigDecimal epfBase = resolveEpfBase(basic, da, totalGross);
        BigDecimal[] pf = calculatePf(input, epfBase);
        BigDecimal epfEmployee = pf[0];
        BigDecimal epfEmployer = pf[1];

        BigDecimal[] esi = calculateEsi(input, totalGross, hasGross);
        BigDecimal esiEmployee = esi[0];
        BigDecimal esiEmployer = esi[1];

        BigDecimal totalSalaryCtc = totalGross.add(epfEmployer).add(esiEmployer);
        BigDecimal pt = calculatePt(input);

        BigDecimal otherManualDeduction = nz(input.getOtherManualDeduction());
        BigDecimal allowance = nz(input.getAllowance());

        BigDecimal advanceRecovery = calculateAdvanceRecovery(input, totalGross, epfEmployee, esiEmployee, pt, otherManualDeduction);
        BigDecimal outstandingAdvance = advanceService.getOutstandingForEmployee(input.getTenantId(), input.getEmployeeId());

        BigDecimal totalDeduct = epfEmployee.add(esiEmployee).add(pt).add(otherManualDeduction).add(advanceRecovery);
        BigDecimal netPayment = calculateNetPay(totalGross, totalDeduct, allowance);

        return new PayrollCalculationResult(basic, da, totalGross, epfEmployee, epfEmployer, esiEmployee, esiEmployer,
                totalSalaryCtc, pt, otherManualDeduction, advanceRecovery, outstandingAdvance, allowance, totalDeduct, netPayment);
    }

    /** STEP 1: structures with no separate Basic/DA (DAILY/HOURLY/CONTRACT) fall back to the full Gross as the PF base. */
    private BigDecimal resolveEpfBase(BigDecimal basic, BigDecimal da, BigDecimal totalGross) {
        BigDecimal basicPlusDa = basic.add(da);
        return basicPlusDa.signum() > 0 ? basicPlusDa : totalGross;
    }

    /** STEP 2 (spec section 11): PF is 0 unless BOTH the tenant's PayrollSettings enable it AND this employee's own pfApplicable flag is true - both are already combined into input.isPfApplicable() by the caller. */
    private BigDecimal[] calculatePf(PayrollCalculationInput input, BigDecimal epfBase) {
        if (!input.isPfApplicable()) {
            return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
        }
        BigDecimal employee = percentOf(epfBase, input.getEpfEmployeePercent());
        BigDecimal employer = percentOf(epfBase, input.getEpfEmployerPercent());
        return new BigDecimal[] { employee, employer };
    }

    /** STEP 3 (spec section 12): ESI is 0 unless applicable AND (no wage ceiling configured OR Gross is within it). An employee with no active Salary Structure this month (hasGross=false) never gets charged ESI, regardless of what a zeroed-out Gross might otherwise compare to. */
    private BigDecimal[] calculateEsi(PayrollCalculationInput input, BigDecimal totalGross, boolean hasGross) {
        boolean eligible = hasGross && input.isEsiApplicable()
                && (input.getEsiWageCeiling() == null || totalGross.compareTo(input.getEsiWageCeiling()) <= 0);
        if (!eligible) {
            return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
        }
        BigDecimal employee = percentOf(totalGross, input.getEsiEmployeePercent());
        BigDecimal employer = percentOf(totalGross, input.getEsiEmployerPercent());
        return new BigDecimal[] { employee, employer };
    }

    /** STEP 4 (spec section 13): flat Professional Tax, 0 unless applicable. Tax (spec section 14) is not yet a distinct input - no tax-applicability configuration exists anywhere in the project yet, so it is not calculated here; it is documented future work, not silently invented. */
    private BigDecimal calculatePt(PayrollCalculationInput input) {
        return input.isPtApplicable() ? nz(input.getProfessionalTaxAmount()) : BigDecimal.ZERO;
    }

    /**
     * STEP 5 (spec section 16): the advance module - not this class - owns "how much recovery
     * applies this month," including which of an employee's possibly-multiple advances it comes
     * from and in what order (see EmployeeAdvanceService.computeMonthlyRecovery). This method
     * only computes the CAP (how much room is left in Gross after every other deduction) and
     * asks the advance module to recover up to that cap - it never duplicates the recovery
     * formula itself.
     */
    private BigDecimal calculateAdvanceRecovery(PayrollCalculationInput input, BigDecimal totalGross,
                                                 BigDecimal epfEmployee, BigDecimal esiEmployee, BigDecimal pt, BigDecimal otherManualDeduction) {
        BigDecimal remainingForAdvance = totalGross.subtract(epfEmployee).subtract(esiEmployee).subtract(pt).subtract(otherManualDeduction).max(BigDecimal.ZERO);
        return advanceService.computeMonthlyRecovery(
                input.getTenantId(), input.getEmployeeId(), input.getYear(), input.getMonth(), input.getPayrollRunId(), remainingForAdvance);
    }

    /** STEP 6 (spec section 19): Net Pay = Total Earnings (Gross + Allowance) - Total Deductions. Never negative in practice, since Advance Recovery is already capped in calculateAdvanceRecovery() above and every other deduction is a fixed/percentage amount that cannot itself exceed Gross under normal configuration. */
    private BigDecimal calculateNetPay(BigDecimal totalGross, BigDecimal totalDeduct, BigDecimal allowance) {
        return totalGross.subtract(totalDeduct).add(allowance);
    }

    private BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        return base.multiply(percent).divide(HUNDRED, SCALE, ROUNDING);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
