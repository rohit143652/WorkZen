-- ============================================================
-- V61: Annual carry-forward reset option for Paid Leave
--
-- reset_annually = TRUE means carry-forward only applies within the same
-- calendar year - the balance automatically resets to 0 every January
-- (see EmployeePaidLeaveService.resolveCarryForward). Default FALSE keeps
-- the existing behaviour: carry-forward continues indefinitely across
-- year boundaries.
-- ============================================================

ALTER TABLE paid_leave_configurations
    ADD COLUMN reset_annually BOOLEAN NOT NULL DEFAULT FALSE AFTER maximum_carry_forward;
