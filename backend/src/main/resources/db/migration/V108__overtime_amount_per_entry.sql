-- ============================================================
-- V108: Overtime amount is entered directly per entry, not a company-wide rate
--
-- payroll_settings.overtime_rate_per_hour (V105) assumed every overtime hour across the whole
-- company is worth the same fixed amount - too rigid in practice (holiday overtime, negotiated
-- rates, etc. can legitimately differ entry to entry). That column is left in place (unused,
-- same "don't drop a column" precedent as the legacy 'overtime' amount field before it) but
-- nothing reads it any more.
--
-- amount on employee_overtime_records is now what an Admin/Supervisor actually types in when
-- logging a day's overtime - hours stays as the informational "how long" record, amount is the
-- real rupee figure payroll sums up. Backfilled to 0.00 for any pre-existing row (there
-- shouldn't be any yet, this table was only just created in V107).
-- ============================================================

ALTER TABLE employee_overtime_records
    ADD COLUMN amount DECIMAL(10,2) NOT NULL DEFAULT 0.00;
