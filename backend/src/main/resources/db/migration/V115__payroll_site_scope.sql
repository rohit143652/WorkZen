-- ============================================================
-- V115: Site-wise Payroll Scope (Phase 4)
--
-- PHASE 4 AUDIT FINDING: PayrollRunCreateRequest had no site scoping at all - createRun() always
-- selected every ACTIVE employee company-wide (findAllByClientCompanyIdAndStatusOrderBy...),
-- with no way to generate payroll for a single site or a chosen subset of sites.
--
-- site_ids stores the scope AS COMMA-SEPARATED SITE IDS directly on the run (empty/NULL = all
-- sites) - same convention as attendance_rule_config.weekly_off_days elsewhere in this codebase.
-- This is deliberately stored on the run itself, not re-derived from a request parameter on
-- every recalculation: Phase 4's "Historical Accuracy" requirement means a run's scope must stay
-- fixed for its whole lifecycle - recalculating a run must use the SAME site scope it was
-- created with, never whatever sites happen to be passed in on a later call.
-- ============================================================

ALTER TABLE payroll_runs
    ADD COLUMN site_ids VARCHAR(500) NULL AFTER client_company_id;

-- Every existing run implicitly covered "all sites" (the only option that ever existed) - NULL
-- here already means exactly that (see PayrollRunService), so no backfill value is needed.
