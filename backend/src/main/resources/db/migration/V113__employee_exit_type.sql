-- ============================================================
-- V113: Exit Type - generalizing employee exits beyond resignation-only
--
-- PHASE 2 AUDIT FINDING: employee_exits was built entirely around voluntary resignation
-- (resignationDate, no way to record an involuntary termination, retirement, or contract end).
-- This baked in an assumption that doesn't hold for every industry this platform must serve
-- (e.g. construction/security/manufacturing companies commonly have disciplinary terminations
-- and fixed-term contract completions, not just resignations).
--
-- resignationDate itself is kept as-is (not renamed) for backward compatibility - every existing
-- row, every existing frontend binding, and every other query already uses that name. It's
-- simply reinterpreted as "exit initiation date" regardless of exit_type going forward.
-- ============================================================

ALTER TABLE employee_exits
    ADD COLUMN exit_type VARCHAR(20) NOT NULL DEFAULT 'RESIGNATION' AFTER employee_id;

-- Every existing row genuinely WAS a resignation (it's the only type this module ever supported),
-- so the DEFAULT above is already correct for all of them - no separate backfill UPDATE needed.
