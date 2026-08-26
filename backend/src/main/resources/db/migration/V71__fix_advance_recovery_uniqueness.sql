-- ============================================================
-- V71: Fix advance_recovery_transactions uniqueness for manual settlements
--
-- The original UNIQUE (advance_id, year, month) from V63 assumed exactly
-- one recovery row could ever exist per advance per month - true when only
-- PAYROLL-sourced rows existed, but V70 introduced MANUAL_SETTLEMENT rows
-- (spec section 17) which can legitimately coexist with a PAYROLL row in
-- the same month (e.g. payroll already recovered its usual amount this
-- month, and the employee separately pays down more of the balance in
-- cash the same month). Without this fix, EmployeeAdvanceService
-- .settlePartial() would throw a duplicate-key error whenever a payroll
-- recovery already existed for that month.
--
-- New constraint allows one row per (advance, month, source) - i.e. at
-- most one PAYROLL row and one MANUAL_SETTLEMENT row per advance per
-- month. Known limitation, accepted for now: two separate manual
-- settlements against the same advance within the same calendar month
-- still collide (the second call updates/replaces the first's amount
-- rather than adding a third row) - documented in
-- EmployeeAdvanceService.settlePartial().
-- ============================================================

ALTER TABLE advance_recovery_transactions
    DROP INDEX uq_advance_recovery_month,
    ADD CONSTRAINT uq_advance_recovery_month_source UNIQUE (advance_id, year, month, source);
