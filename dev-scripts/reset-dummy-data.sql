-- ============================================================
-- reset-dummy-data.sql  (MANUAL SCRIPT - NOT a Flyway migration)
--
-- Run this by hand, whenever you want a clean slate to demo/test against,
-- for the sample tenant CLI0001 and its EMP0001-EMP0010 employees. This
-- is deliberately NOT a V-numbered migration file: a Flyway migration
-- runs exactly once, automatically, on every environment forever -
-- including a real customer's production database someday. A "wipe test
-- data" script must never be something that runs itself.
--
-- What this clears (and why it's safe to clear):
--   1. employee_paid_leave_balances for the months you've been testing
--      against (2026-07, 2026-08) - these get recomputed fresh, from
--      scratch, using whatever Paid Leave / Payroll Settings policy is
--      CURRENTLY active, the next time you open a report or calculate
--      payroll for that month. This is exactly the "stale balance
--      already committed under an old policy" issue from your last
--      question - clearing it is what lets a policy change (like
--      switching Paid Leave to Inactive) actually take visible effect
--      for a month you already looked at.
--   2. payroll_run_employees + payroll_runs for the same test months -
--      so Payroll Processing starts clean too (no half-tested Calculated/
--      Approved runs left over). advance_recovery_transactions with
--      source='PAYROLL' for those months are cleaned up too, since they
--      only ever existed because of those payroll runs - MANUAL_SETTLEMENT
--      rows (real "employee paid in cash" records) are left completely
--      alone, since those aren't test artifacts.
--
-- What this does NOT touch (left exactly as configured):
--   - Payroll Settings history, Paid Leave Policy history - your PF/ESI/PT
--     rates and Paid Leave on/off decisions stay exactly as you set them.
--   - Employee Advances themselves (amounts, recovery settings) - only
--     the PAYROLL-sourced recovery transactions tied to the cleared runs.
--   - Salary structures, employee records, attendance marking.
--
-- Adjust the year/month list in the WHERE clauses below if you want to
-- reset a different test month than July/August 2026.
-- ============================================================

SET @tenant_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001');

-- Step 1: drop PAYROLL-sourced advance recovery tied to the payroll runs we're about to clear -
-- MANUAL_SETTLEMENT rows are untouched.
DELETE art FROM advance_recovery_transactions art
JOIN payroll_runs pr ON pr.id = art.payroll_run_id
WHERE pr.client_company_id = @tenant_id
  AND ((pr.year = 2026 AND pr.month = 7) OR (pr.year = 2026 AND pr.month = 8))
  AND art.source = 'PAYROLL';

-- Step 2: clear the test payroll runs (and their employee snapshot rows via ON DELETE CASCADE).
DELETE FROM payroll_runs
WHERE client_company_id = @tenant_id
  AND ((year = 2026 AND month = 7) OR (year = 2026 AND month = 8));

-- Step 3: clear the committed leave balances for those same months, so they recompute fresh
-- under whatever Paid Leave policy is active right now.
DELETE FROM employee_paid_leave_balances
WHERE client_company_id = @tenant_id
  AND ((year = 2026 AND month = 7) OR (year = 2026 AND month = 8));

-- Step 4: sanity check - confirm the clear worked and see what's left for these months.
SELECT 'payroll_runs remaining' AS check_name, COUNT(*) AS count FROM payroll_runs
WHERE client_company_id = @tenant_id AND ((year = 2026 AND month = 7) OR (year = 2026 AND month = 8))
UNION ALL
SELECT 'employee_paid_leave_balances remaining', COUNT(*) FROM employee_paid_leave_balances
WHERE client_company_id = @tenant_id AND ((year = 2026 AND month = 7) OR (year = 2026 AND month = 8));
