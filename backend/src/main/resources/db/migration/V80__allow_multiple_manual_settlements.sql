-- ============================================================
-- V80: Allow multiple MANUAL_SETTLEMENT entries per advance per month
--
-- V71's UNIQUE (advance_id, year, month, source) constraint meant a
-- second manual settlement against the same advance in the same calendar
-- month would silently merge into the first row's amount rather than
-- becoming its own entry - so paying an advance down twice in one day
-- only ever showed up as one history line. That was a known, documented
-- limitation; this closes it by simply removing the composite constraint.
--
-- PAYROLL idempotency (never double-recovering the same advance+month) is
-- enforced at the APPLICATION level instead, in EmployeeAdvanceService
-- .computeMonthlyRecovery(): it looks up any existing PAYROLL row for
-- that (advance, year, month) via findByAdvanceIdAndYearAndMonthAndSource()
-- and updates it in place rather than inserting a second one - see that
-- method for the exact find-or-create logic. An earlier version of this
-- migration also tried to enforce that same guarantee at the database
-- level via a generated column + a second unique index, but that
-- triggered a MySQL error (1215, "Cannot add foreign key constraint")
-- on some MySQL 8.0 builds when combining a STORED generated column with
-- a table that already has foreign keys - not worth the fragility for a
-- belt-and-suspenders guarantee the application layer already provides.
--
-- DEFENSIVE / IDEMPOTENT: every step checks information_schema first, so
-- this migration is safe to re-run on a database where an earlier
-- attempt partially applied.
-- ============================================================

-- Step 0: the fk_advance_recovery_advance foreign key on advance_id has never had its own
-- index - it has always relied on uq_advance_recovery_month_source (advance_id is its leftmost
-- column) as its supporting index, since V63 never created a standalone one. MySQL/InnoDB
-- refuses to drop an index still needed by a foreign key, so a plain index on advance_id alone
-- must exist FIRST, before the old composite unique constraint can be dropped in Step 1.
SET @plain_idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'advance_recovery_transactions'
      AND INDEX_NAME = 'idx_advance_recovery_advance_id'
);
SET @sql = IF(@plain_idx_exists = 0,
    'CREATE INDEX idx_advance_recovery_advance_id ON advance_recovery_transactions (advance_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 1: drop the old constraint, only if it still exists. Nothing replaces it - see the
-- header comment above for why the application layer is responsible for PAYROLL idempotency.
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'advance_recovery_transactions'
      AND INDEX_NAME = 'uq_advance_recovery_month_source'
);
SET @sql = IF(@idx_exists > 0,
    'ALTER TABLE advance_recovery_transactions DROP INDEX uq_advance_recovery_month_source',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2 (cleanup, not strictly required once the constraint above is gone, but harmless):
-- merge any pre-existing duplicate PAYROLL rows for the same (advance, year, month) that may
-- already exist from earlier testing, so history views don't show stale duplicate entries.
UPDATE advance_recovery_transactions t
JOIN (
    SELECT advance_id, year, month, MIN(id) AS keep_id, SUM(recovered_amount) AS total_amount
    FROM advance_recovery_transactions
    WHERE source = 'PAYROLL'
    GROUP BY advance_id, year, month
    HAVING COUNT(*) > 1
) dupes ON t.id = dupes.keep_id
SET t.recovered_amount = dupes.total_amount;

DELETE t FROM advance_recovery_transactions t
JOIN (
    SELECT advance_id, year, month, MIN(id) AS keep_id
    FROM advance_recovery_transactions
    WHERE source = 'PAYROLL'
    GROUP BY advance_id, year, month
    HAVING COUNT(*) > 1
) dupes ON t.advance_id = dupes.advance_id AND t.year = dupes.year AND t.month = dupes.month
    AND t.source = 'PAYROLL' AND t.id <> dupes.keep_id;
