-- ============================================================
-- V58: Drop unused hra_percent column from payroll_settings
--
-- Payroll Register was folded into the Monthly Attendance & Payment
-- Report (one report instead of two, per user request to simplify the
-- flow). The merged report gets its gross figure directly from the
-- employee's Salary Structure, so a separate HRA% setting was never
-- actually applied - keeping it around would just be a config option
-- that silently does nothing, which is its own source of confusion.
-- ============================================================

ALTER TABLE payroll_settings DROP COLUMN hra_percent;
