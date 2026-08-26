-- ============================================================
-- V83: End-to-end test dummy data for everything built this session
--
-- Adds sample data specifically exercising the newer features so a fresh
-- database (V1-V82 run clean) has something realistic to click through
-- immediately, without the admin needing to manually create test records
-- for every feature first:
--
--   - Employee-level PF/ESI/PT applicability override (V64/EMP form
--     toggle) - EMP0003 and EMP0008 are set to fully exempt, everyone
--     else keeps the TRUE default, so Payroll Calculate visibly shows
--     the difference.
--   - Employee Advance "Cut from Payroll" pause switch (recoverViaPayroll)
--     - EMP0002's advance is paused, so Calculate visibly skips it.
--   - Advance Recovery Start Month - EMP0004's advance doesn't start
--     recovering until next month, so this month's Calculate visibly
--     skips it too, and Advance History shows "Recovery Starts" in the
--     future.
--   - Mixed recovery sources on one advance (EMP0001's has one Manual
--     Settlement already recorded) - so its Recovery History modal has
--     more than one row/source to look at immediately.
--
-- All amounts/dates deliberately match the ones already used earlier in
-- this session's manual testing, so this reads as a continuation of the
-- same demo story, not a disconnected new dataset.
-- ============================================================

SET @tenant_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001');

-- ---- Employee-level PF/ESI/PT applicability overrides ----
UPDATE employees SET pf_applicable = FALSE, esi_applicable = FALSE, pt_applicable = FALSE
WHERE client_company_id = @tenant_id AND employee_code = 'EMP0003';

UPDATE employees SET pf_applicable = FALSE, esi_applicable = FALSE, pt_applicable = FALSE
WHERE client_company_id = @tenant_id AND employee_code = 'EMP0008';

-- ---- Sample advances (only inserted if this tenant doesn't already have any - keeps this
-- migration safe to have run alongside whatever manual testing already happened) ----

-- EMP0001 (Rohit Patil): small advance, normal payroll recovery, already-mixed history.
INSERT INTO employee_advances (client_company_id, employee_id, advance_date, amount, reason, payment_mode,
                                monthly_recovery_amount, recovery_start_year, recovery_start_month, remarks,
                                recover_via_payroll, status)
SELECT @tenant_id, e.id, '2026-08-01', 2000.00, 'Sample Advance', 'CASH',
       500.00, 2026, 8, 'Seed data for end-to-end testing', TRUE, 'ACTIVE'
FROM employees e
WHERE e.client_company_id = @tenant_id AND e.employee_code = 'EMP0001'
  AND NOT EXISTS (SELECT 1 FROM employee_advances a WHERE a.employee_id = e.id AND a.reason = 'Sample Advance');

INSERT INTO advance_recovery_transactions (client_company_id, employee_id, advance_id, year, month, recovered_amount, source, created_by)
SELECT @tenant_id, e.id, adv.id, 2026, 8, 300.00, 'MANUAL_SETTLEMENT', NULL
FROM employee_advances adv
JOIN employees e ON e.id = adv.employee_id
WHERE e.client_company_id = @tenant_id AND e.employee_code = 'EMP0001' AND adv.reason = 'Sample Advance'
  AND NOT EXISTS (SELECT 1 FROM advance_recovery_transactions t WHERE t.advance_id = adv.id AND t.source = 'MANUAL_SETTLEMENT');

-- EMP0002 (Amit Sharma): larger advance, payroll recovery deliberately PAUSED (recoverViaPayroll =
-- FALSE) - demonstrates "employee already paid this month in cash, skip payroll this run".
INSERT INTO employee_advances (client_company_id, employee_id, advance_date, amount, reason, payment_mode,
                                monthly_recovery_amount, recovery_start_year, recovery_start_month, remarks,
                                recover_via_payroll, status)
SELECT @tenant_id, e.id, '2026-08-01', 10000.00, 'Sample Advance - Payroll Paused', 'BANK_TRANSFER',
       2000.00, 2026, 8, 'Seed data - recoverViaPayroll intentionally FALSE for demo', FALSE, 'ACTIVE'
FROM employees e
WHERE e.client_company_id = @tenant_id AND e.employee_code = 'EMP0002'
  AND NOT EXISTS (SELECT 1 FROM employee_advances a WHERE a.employee_id = e.id AND a.reason = 'Sample Advance - Payroll Paused');

-- EMP0004 (Prakash Jadhav): advance with recovery not starting until next month.
INSERT INTO employee_advances (client_company_id, employee_id, advance_date, amount, reason, payment_mode,
                                monthly_recovery_amount, recovery_start_year, recovery_start_month, remarks,
                                recover_via_payroll, status)
SELECT @tenant_id, e.id, '2026-08-01', 5000.00, 'Sample Advance - Future Recovery Start', 'CASH',
       1000.00, 2026, 9, 'Seed data - recovery deliberately starts next month for demo', TRUE, 'ACTIVE'
FROM employees e
WHERE e.client_company_id = @tenant_id AND e.employee_code = 'EMP0004'
  AND NOT EXISTS (SELECT 1 FROM employee_advances a WHERE a.employee_id = e.id AND a.reason = 'Sample Advance - Future Recovery Start');
