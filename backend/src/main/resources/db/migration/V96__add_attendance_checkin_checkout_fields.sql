-- ============================================================
-- V96: Check-in/check-out time-tracking fields on attendance
--
-- Purely additive - every column here is nullable, so every existing row
-- (marked via the old one-click "mark PRESENT/ABSENT/HALF_DAY/ON_LEAVE"
-- flow) stays exactly as valid as it was before this migration. That old
-- flow keeps working unchanged for any site/employee that doesn't use
-- precise time-tracking - it simply leaves these new columns null.
--
-- attendance_source backfill: the only way to tell an existing row was
-- self-marked (vs. admin-marked) is the exact remarks text
-- AttendanceService.markMine() has always written ("Self-marked") - anything
-- else defaults to ADMIN, which is accurate for every mark()/bulkMark() row,
-- and reasonably close for the Leave/Holiday auto-marked rows (those are
-- SYSTEM in spirit, but ADMIN is the safer/more conservative default since
-- SYSTEM implies "no human accountable", and a real person approved the
-- leave/created the holiday that triggered them).
-- ============================================================

ALTER TABLE attendance
    ADD COLUMN check_in_time        DATETIME     NULL,
    ADD COLUMN check_out_time       DATETIME     NULL,
    ADD COLUMN gross_work_minutes   INT          NULL,
    ADD COLUMN break_minutes        INT          NULL,
    ADD COLUMN net_work_minutes     INT          NULL,
    ADD COLUMN work_mode            VARCHAR(20)  NULL,
    ADD COLUMN attendance_source    VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    ADD COLUMN is_late              BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN late_minutes         INT          NULL,
    ADD COLUMN is_early_exit        BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN early_exit_minutes   INT          NULL,
    ADD COLUMN check_out_latitude   DECIMAL(10, 7) NULL,
    ADD COLUMN check_out_longitude  DECIMAL(10, 7) NULL;

UPDATE attendance SET attendance_source = 'SELF' WHERE remarks = 'Self-marked';

CREATE INDEX idx_attendance_source ON attendance (client_company_id, attendance_source);
