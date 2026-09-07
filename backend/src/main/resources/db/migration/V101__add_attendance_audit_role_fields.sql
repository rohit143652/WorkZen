-- ============================================================
-- V101: Attendance audit enrichment - who (by role) created/modified a record, and why
--
-- attendance_source (V96) already records HOW a record was created (SELF/ADMIN/SYSTEM), but not
-- WHICH role of admin did it (a CLIENT_ADMIN, a SITE_SUPERVISOR, and HR_ADMIN are all just
-- "ADMIN" today) or WHY a later correction was made. This adds that without touching any
-- existing column - every new column is nullable, so every existing row stays valid.
-- ============================================================

ALTER TABLE attendance
    ADD COLUMN created_by_role    VARCHAR(30) NULL,
    ADD COLUMN modified_by_role   VARCHAR(30) NULL,
    ADD COLUMN modification_reason VARCHAR(255) NULL;
