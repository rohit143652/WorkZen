-- ============================================================
-- V54: Drop the superseded ad-hoc paid-leave tables
--
-- Before the dedicated Paid Leave Management module (leave_module) existed,
-- a simpler paid-leave mechanism lived directly in attendance_module:
-- attendance_settings (tenant default), employee_paid_leave_overrides
-- (per-employee rate override), and employee_leave_ledger (monthly
-- balance). All of that is now superseded by paid_leave_configurations,
-- employee_extra_paid_leaves, and employee_paid_leave_balances (V50-52),
-- which match the Paid Leave Management spec exactly and are the only
-- system MonthlyAttendanceReportService now reads from. Keeping both
-- systems around would recreate the same "two places leave lives"
-- confusion this project has already run into once with Designation-based
-- pay vs. Salary Structures.
-- ============================================================

DROP TABLE IF EXISTS employee_leave_ledger;
DROP TABLE IF EXISTS employee_paid_leave_overrides;
DROP TABLE IF EXISTS attendance_settings;
