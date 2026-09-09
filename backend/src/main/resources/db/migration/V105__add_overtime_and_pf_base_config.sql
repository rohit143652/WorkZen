-- ============================================================
-- V105: Overtime configuration + configurable PF calculation base
--
-- overtime_enabled/overtime_rate_per_hour go on payroll_settings, the SAME effective-dated,
-- history-capable table every other client-configured payroll rate already lives on (EPF/ESI/PT
-- percentages) - editing "today's" overtime rate creates a new row the same way, never mutating
-- a past month's already-applicable rate (see PayrollSettings entity javadoc).
--
-- pf_calculation_base is the client's choice of what PF is a percentage OF: GROSS, BASIC, or
-- BASIC_PLUS_DA. Defaults to 'BASIC_PLUS_DA' for every existing row, which is EXACTLY the
-- hardcoded behavior PayrollCalculationService.resolveEpfBase() used before this migration - no
-- existing tenant's payroll numbers change because of this default.
--
-- overtime_hours on employee_payroll_adjustments is the per-employee-per-month hours entry the
-- rate above is multiplied against - see PayrollInputResolver for where that multiplication
-- happens. The pre-existing 'overtime' column on that same table was always an unused, never-
-- wired amount field (confirmed: nothing in the frontend or backend ever wrote to it) - left in
-- place rather than dropped, since dropping a column is a one-way door and it costs nothing to
-- leave sitting at its default of 0.00 for every row.
-- ============================================================

ALTER TABLE payroll_settings
    ADD COLUMN overtime_enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN overtime_rate_per_hour DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN pf_calculation_base    VARCHAR(20)  NOT NULL DEFAULT 'BASIC_PLUS_DA';

ALTER TABLE employee_payroll_adjustments
    ADD COLUMN overtime_hours DECIMAL(6,2) NOT NULL DEFAULT 0.00;

-- Persisted snapshot of the computed overtime amount AND the raw hours it came from, for each
-- calculated PayrollRunEmployee row - the hours are needed so the adjustment-edit form can show
-- what was actually entered last time, not just the resulting rupee figure (which alone can't be
-- reversed back into hours if the rate has since changed).
ALTER TABLE payroll_run_employees
    ADD COLUMN overtime_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN overtime_hours  DECIMAL(6,2)  NOT NULL DEFAULT 0.00;
