package com.example.application.payroll_module.service;

import com.example.application.attendance_module.entity.Attendance;
import com.example.application.leave_module.entity.EmployeePaidLeaveBalance;
import com.example.application.leave_module.service.EmployeePaidLeaveService;
import com.example.application.payroll_module.dto.EmployeePayrollInputs;
import com.example.application.employee_module.entity.Employee;
import com.example.application.salary_structure_module.dto.EmployeeSalaryStructureResponse;
import com.example.application.salary_structure_module.dto.SalaryStructureComponentResponse;
import com.example.application.salary_structure_module.dto.SalaryStructureResponse;
import com.example.application.salary_structure_module.service.EmployeeSalaryStructureService;
import com.example.application.salary_structure_module.service.SalaryStructureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Gathers one employee's attendance + paid-leave + salary-structure facts
 * for a given month, BEFORE any deduction math - this is the "what
 * happened and what does the structure say they earn" step shared by the
 * attendance report and the persisted Payroll Run, extracted so this
 * gathering logic exists in exactly one place (same principle as
 * PayrollCalculationService for the deduction math in Phase 1).
 *
 * Two entry points, same math, different commit behaviour (architecture
 * refactor Phase 4):
 *   - resolveEmployeeInputs() WRITES - calls EmployeePaidLeaveService
 *     .resolveMonth()/.recordUsage(), upserting a leave-balance row. Only
 *     PayrollRunService.calculateRun() should call this - an explicit,
 *     user-initiated "calculate payroll" action.
 *   - previewEmployeeInputs() is READ-ONLY - calls EmployeePaidLeaveService
 *     .previewMonth() instead, which never writes. This is what
 *     attendance_module.MonthlyAttendanceReportService uses, so simply
 *     viewing the attendance report can never mutate Leave data.
 */
@Service
public class PayrollInputResolver {

    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal STANDARD_HOURS_PER_DAY = BigDecimal.valueOf(8);

    private final EmployeePaidLeaveService paidLeaveService;
    private final EmployeeSalaryStructureService employeeSalaryStructureService;
    private final SalaryStructureService salaryStructureService;

    public PayrollInputResolver(EmployeePaidLeaveService paidLeaveService,
                                 EmployeeSalaryStructureService employeeSalaryStructureService,
                                 SalaryStructureService salaryStructureService) {
        this.paidLeaveService = paidLeaveService;
        this.employeeSalaryStructureService = employeeSalaryStructureService;
        this.salaryStructureService = salaryStructureService;
    }

    /**
     * @param structureCache shared across a whole report/run so the same Salary Structure
     *                       is only fetched once even though many employees may share it.
     */
    @Transactional
    public EmployeePayrollInputs resolveEmployeeInputs(Long tenantId, Employee employee, int year, int month,
                                                        LocalDate monthEnd, int daysInMonth,
                                                        List<Attendance> attendanceForEmployee,
                                                        Map<Long, SalaryStructureResponse> structureCache) {
        return resolveOrPreview(tenantId, employee, year, month, monthEnd, daysInMonth, attendanceForEmployee, structureCache, true);
    }

    /**
     * Read-only counterpart (architecture refactor Phase 4) - identical
     * attendance-counting and Salary Structure resolution, but never calls
     * EmployeePaidLeaveService.resolveMonth()/recordUsage(), so it never
     * creates or modifies a leave-balance row. This is what
     * attendance_module.MonthlyAttendanceReportService uses so that simply
     * viewing the Monthly Attendance Report can never write Leave data -
     * only an explicit Payroll Run calculation (resolveEmployeeInputs
     * above) commits anything.
     */
    @Transactional(readOnly = true)
    public EmployeePayrollInputs previewEmployeeInputs(Long tenantId, Employee employee, int year, int month,
                                                        LocalDate monthEnd, int daysInMonth,
                                                        List<Attendance> attendanceForEmployee,
                                                        Map<Long, SalaryStructureResponse> structureCache) {
        return resolveOrPreview(tenantId, employee, year, month, monthEnd, daysInMonth, attendanceForEmployee, structureCache, false);
    }

    private EmployeePayrollInputs resolveOrPreview(Long tenantId, Employee employee, int year, int month,
                                                    LocalDate monthEnd, int daysInMonth,
                                                    List<Attendance> attendanceForEmployee,
                                                    Map<Long, SalaryStructureResponse> structureCache,
                                                    boolean commit) {
        EmployeePayrollInputs inputs = new EmployeePayrollInputs();

        long present = attendanceForEmployee.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long halfDay = attendanceForEmployee.stream().filter(a -> "HALF_DAY".equals(a.getStatus())).count();
        long onLeave = attendanceForEmployee.stream().filter(a -> "ON_LEAVE".equals(a.getStatus())).count();
        long absent = attendanceForEmployee.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();

        BigDecimal monthlyAllocation;
        BigDecimal carryForward;
        BigDecimal extraLeave;
        BigDecimal paidLeave;
        boolean manualOverride;

        if (commit) {
            EmployeePaidLeaveBalance balance = paidLeaveService.resolveMonth(tenantId, employee.getId(), year, month);
            BigDecimal availableBeforeUsage = balance.getMonthlyAllocation().add(balance.getCarryForward()).add(balance.getExtraLeave());
            if (balance.isManualOverride()) {
                paidLeave = balance.getUsedLeave();
            } else {
                paidLeave = availableBeforeUsage.max(BigDecimal.ZERO).min(BigDecimal.valueOf(onLeave));
                balance = paidLeaveService.recordUsage(tenantId, employee.getId(), year, month, paidLeave);
            }
            monthlyAllocation = balance.getMonthlyAllocation();
            carryForward = balance.getCarryForward();
            extraLeave = balance.getExtraLeave();
            manualOverride = balance.isManualOverride();
            inputs.setLeaveBalanceClosing(balance.getAvailableLeave());
        } else {
            // Read-only: EmployeePaidLeaveService.previewMonth() returns the existing balance row
            // if one was already committed by a Payroll Run calculation, or a computed-but-never-saved
            // preview otherwise - either way, nothing is written here.
            var preview = paidLeaveService.previewMonth(employee.getId(), year, month);
            monthlyAllocation = preview.getMonthlyAllocation();
            carryForward = preview.getCarryForward();
            extraLeave = preview.getExtraLeave();
            manualOverride = preview.isManualOverride();
            BigDecimal availableBeforeUsage = monthlyAllocation.add(carryForward).add(extraLeave);
            paidLeave = manualOverride ? preview.getUsedLeave() : availableBeforeUsage.max(BigDecimal.ZERO).min(BigDecimal.valueOf(onLeave));
            inputs.setLeaveBalanceClosing(manualOverride ? preview.getAvailableLeave() : availableBeforeUsage.subtract(paidLeave));
        }

        BigDecimal unpaidLeave = BigDecimal.valueOf(onLeave).subtract(paidLeave);
        BigDecimal payableDays = BigDecimal.valueOf(present)
                .add(BigDecimal.valueOf(halfDay).multiply(HALF))
                .add(paidLeave);

        inputs.setPresentDays(present);
        inputs.setHalfDays(halfDay);
        inputs.setOnLeaveDays(onLeave);
        inputs.setAbsentDays(absent);
        inputs.setPaidLeaveDays(paidLeave);
        inputs.setUnpaidLeaveDays(unpaidLeave);
        inputs.setPayableDays(payableDays);
        inputs.setLeaveBalanceOpening(carryForward);
        inputs.setManualLeaveOverride(manualOverride);

        EmployeeSalaryStructureResponse current = employeeSalaryStructureService.getActiveSalaryStructure(employee.getId(), monthEnd).orElse(null);
        BigDecimal basic = BigDecimal.ZERO;
        BigDecimal da = BigDecimal.ZERO;
        if (current == null) {
            inputs.setNote("No salary structure assigned");
        } else {
            inputs.setStructureName(current.getStructureName());
            inputs.setSalaryType(current.getSalaryType());
            SalaryStructureResponse full = structureCache.computeIfAbsent(
                    current.getSalaryStructureId(), salaryStructureService::findById);
            BigDecimal fullMonthBasic = findSalaryComponent(full, "BASIC");
            BigDecimal fullMonthDa = findSalaryComponent(full, "DA");
            basic = fullMonthBasic;
            da = fullMonthDa;
            switch (current.getSalaryType()) {
                case "DAILY" -> {
                    inputs.setRate(full.getDailyRate());
                    inputs.setTotalGross(safeMultiply(inputs.getRate(), payableDays));
                    inputs.setFullGrossEntitlement(safeMultiply(inputs.getRate(), BigDecimal.valueOf(daysInMonth)));
                }
                case "HOURLY" -> {
                    inputs.setRate(full.getHourlyRate());
                    inputs.setTotalGross(safeMultiply(safeMultiply(inputs.getRate(), payableDays), STANDARD_HOURS_PER_DAY));
                    inputs.setFullGrossEntitlement(safeMultiply(safeMultiply(inputs.getRate(), BigDecimal.valueOf(daysInMonth)), STANDARD_HOURS_PER_DAY));
                    inputs.setNote("Hourly rate x assumed 8 hrs/day (attendance tracks days, not hours)");
                }
                case "CONTRACT" -> {
                    inputs.setTotalGross(current.getGrossEarnings());
                    inputs.setFullGrossEntitlement(current.getGrossEarnings());
                    inputs.setNote("Fixed contract amount - not prorated by attendance");
                }
                default -> { // MONTHLY
                    BigDecimal gross = current.getGrossEarnings();
                    inputs.setRate(gross == null ? null : gross.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP));
                    inputs.setTotalGross(safeMultiply(inputs.getRate(), payableDays));
                    inputs.setFullGrossEntitlement(gross);
                    // BUG FIX (verified against SP_College_Aug-26.xlsx reference): Basic and DA
                    // must be prorated by this same payableDays/daysInMonth ratio before being
                    // used as PF's calculation base - previously they stayed at their full-month
                    // entitlement regardless of attendance, so PF was charged on the full month's
                    // Basic+DA even for an employee who worked only part of it. The blended Gross
                    // above was already correct (prorating it directly is mathematically
                    // equivalent to prorating Basic+DA and then adding a percentage-based HRA on
                    // the prorated figure), but Basic and DA individually were never prorated,
                    // and PayrollCalculationService.resolveEpfBase() reads them directly.
                    BigDecimal fullBasicDa = fullMonthBasic.add(fullMonthDa);
                    if (fullBasicDa.signum() > 0 && daysInMonth > 0) {
                        BigDecimal proratedBasicDa = fullBasicDa
                                .divide(BigDecimal.valueOf(daysInMonth), 10, RoundingMode.HALF_UP)
                                .multiply(payableDays)
                                .setScale(2, RoundingMode.HALF_UP);
                        // Split back into Basic/DA in their original proportion so each remains
                        // independently available (e.g. a payslip line showing them separately)
                        // while their sum exactly equals the prorated figure used for PF - splitting
                        // after prorating, rather than prorating each separately and summing,
                        // avoids the two independently-rounded parts drifting a paisa apart.
                        basic = proratedBasicDa.multiply(fullMonthBasic).divide(fullBasicDa, 2, RoundingMode.HALF_UP);
                        da = proratedBasicDa.subtract(basic);
                    } else {
                        basic = BigDecimal.ZERO;
                        da = BigDecimal.ZERO;
                    }
                }
            }
            if (unpaidLeave.signum() > 0 && inputs.getNote().isEmpty()) {
                inputs.setNote(unpaidLeave + " unpaid leave day(s) beyond the available paid-leave balance ("
                        + carryForward + " carried forward + " + monthlyAllocation
                        + " monthly + " + extraLeave + " extra)");
            }
            if (manualOverride && inputs.getNote().isEmpty()) {
                inputs.setNote("Paid leave manually adjusted for this month");
            }
        }
        inputs.setBasicSalary(basic);
        inputs.setDa(da);
        return inputs;
    }

    private static BigDecimal findSalaryComponent(SalaryStructureResponse structure, String code) {
        return structure.getComponents().stream()
                .filter(c -> code.equalsIgnoreCase(c.getComponentCode()))
                .map(SalaryStructureComponentResponse::getResolvedAmount)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    private static BigDecimal safeMultiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        return a.multiply(b).setScale(2, RoundingMode.HALF_UP);
    }
}
