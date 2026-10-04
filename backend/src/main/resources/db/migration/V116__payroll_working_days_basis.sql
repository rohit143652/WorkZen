-- ============================================================
-- V116: Configurable payroll working-days basis
--
-- AUDIT FINDING (SP College Aug-26.xlsx reference): daysInMonth was hardcoded to
-- yearMonth.lengthOfMonth() (always calendar days) everywhere it was used for payroll proration -
-- there was no way for a company to instead use "working days" (calendar days minus configured
-- weekly offs), or a fixed convention like 26 or 30 days, both common in Indian payroll practice.
--
-- working_days_basis: CALENDAR_DAYS (default - every existing row's prior, only behavior, so this
-- default preserves it exactly), WORKING_DAYS (calendar days minus that month's weekly-off days,
-- per the company's existing AttendanceRuleConfig.weekly_off_days), FIXED (a company-chosen
-- constant, stored in fixed_working_days - common conventions are 26 or 30, but stored as a plain
-- configurable number rather than a two-value enum, since "26 or 30" in the spec are examples, not
-- an exhaustive list).
-- ============================================================

ALTER TABLE payroll_settings
    ADD COLUMN working_days_basis VARCHAR(20) NOT NULL DEFAULT 'CALENDAR_DAYS' AFTER pf_calculation_base,
    ADD COLUMN fixed_working_days INT NULL AFTER working_days_basis;
