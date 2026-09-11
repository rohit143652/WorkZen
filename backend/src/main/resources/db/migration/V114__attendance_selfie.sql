-- ============================================================
-- V114: Attendance Selfie Support (Phase 3)
--
-- Storage decision: base64 in the database, matching the EXISTING employees.photo_data pattern
-- exactly (see V1x employee photo migration) - no object/file storage service exists anywhere in
-- this codebase to store images externally instead, and introducing one is a much larger
-- infrastructure change than this feature warrants. LONGTEXT is used (not TEXT) since a selfie
-- is meaningfully larger than a profile photo and two may be stored per attendance row
-- (check-in + check-out).
-- ============================================================

ALTER TABLE attendance_rule_config
    ADD COLUMN check_in_selfie_required  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN check_out_selfie_required BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE attendance
    ADD COLUMN check_in_selfie_data  LONGTEXT NULL,
    ADD COLUMN check_out_selfie_data LONGTEXT NULL;
