package com.example.application.client_company_module.feature;

import java.util.List;

/**
 * Centralized feature codes for company-wise module/feature access (Super Admin controlled).
 * Adding a new one is exactly this: one new constant here, one row in FeatureCatalog's category
 * grouping for the UI, and a FeatureAccessService.isEnabled(tenantId, CODE) check wherever that
 * feature's entry point is (controller/service) - no schema change, no new table.
 *
 * Only the Attendance-related codes below are actually ENFORCED anywhere yet (see
 * AttendanceService/AttendanceController for the checks). The others exist as real, storable,
 * toggleable rows (so the Manage Features screen and this constants list are already correct
 * and future-proof), but nothing currently reads them to restrict access - wiring each one up is
 * the same pattern as Attendance, just not done for every module in this pass.
 */
public final class FeatureCode {

    private FeatureCode() {}

    // ---- Enforced today (see AttendanceService/AttendanceController) ----
    public static final String ATTENDANCE_MANAGEMENT = "ATTENDANCE_MANAGEMENT";
    public static final String EMPLOYEE_SELF_ATTENDANCE = "EMPLOYEE_SELF_ATTENDANCE";
    public static final String ADMIN_MANUAL_ATTENDANCE = "ADMIN_MANUAL_ATTENDANCE";
    public static final String SUPERVISOR_MANUAL_ATTENDANCE = "SUPERVISOR_MANUAL_ATTENDANCE";
    public static final String HR_MANUAL_ATTENDANCE = "HR_MANUAL_ATTENDANCE";

    /** Enforced in PayrollInputResolver/PayrollSettingsService - overtime pay only ever gets
        computed/configured for a company the Super Admin has explicitly turned this on for
        (see V106 migration for why it defaults OFF, unlike the Attendance codes above). */
    public static final String OVERTIME_MANAGEMENT = "OVERTIME_MANAGEMENT";

    // ---- Defined and toggleable, not yet wired to an actual access check anywhere ----
    public static final String EMPLOYEE_MANAGEMENT = "EMPLOYEE_MANAGEMENT";
    public static final String LEAVE_MANAGEMENT = "LEAVE_MANAGEMENT";
    public static final String SALARY_MANAGEMENT = "SALARY_MANAGEMENT";
    public static final String PAYROLL = "PAYROLL";
    public static final String SHIFT_MANAGEMENT = "SHIFT_MANAGEMENT";
    public static final String HOLIDAY_CALENDAR = "HOLIDAY_CALENDAR";
    public static final String REPORTS = "REPORTS";
    public static final String EXPENSE_MANAGEMENT = "EXPENSE_MANAGEMENT";
    public static final String RECRUITMENT = "RECRUITMENT";
    public static final String ASSET_MANAGEMENT = "ASSET_MANAGEMENT";

    /** Codes actually enforced in the backend today - drives which toggles the Manage Features UI marks as "live" vs "coming soon". */
    public static final List<String> ENFORCED = List.of(
            ATTENDANCE_MANAGEMENT, EMPLOYEE_SELF_ATTENDANCE, ADMIN_MANUAL_ATTENDANCE,
            SUPERVISOR_MANUAL_ATTENDANCE, HR_MANUAL_ATTENDANCE, OVERTIME_MANAGEMENT);

    /** Every known code, grouped by category, in the order the Manage Features screen should render them. */
    public static final List<FeatureCategory> CATALOG = List.of(
            new FeatureCategory("Core HR", List.of(EMPLOYEE_MANAGEMENT, LEAVE_MANAGEMENT, RECRUITMENT)),
            new FeatureCategory("Attendance", List.of(
                    ATTENDANCE_MANAGEMENT, EMPLOYEE_SELF_ATTENDANCE, ADMIN_MANUAL_ATTENDANCE,
                    SUPERVISOR_MANUAL_ATTENDANCE, HR_MANUAL_ATTENDANCE, SHIFT_MANAGEMENT, HOLIDAY_CALENDAR)),
            new FeatureCategory("Payroll & Finance", List.of(SALARY_MANAGEMENT, PAYROLL, OVERTIME_MANAGEMENT, EXPENSE_MANAGEMENT)),
            new FeatureCategory("Other", List.of(REPORTS, ASSET_MANAGEMENT))
    );

    public record FeatureCategory(String label, List<String> codes) {}
}
