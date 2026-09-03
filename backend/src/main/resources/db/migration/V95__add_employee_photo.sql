-- ============================================================
-- V95: Employee Photo
--
-- Stored directly as a base64 data-URI string (e.g. "data:image/jpeg;base64,...") rather than as
-- a file on disk - this app has no separate file/object storage service (S3 etc.) set up, and a
-- single compressed photo is small enough that storing it as text in the row itself is simple
-- and avoids needing to stand up static file serving just for this. The frontend is responsible
-- for resizing/compressing before upload (see the photo-capture component) so this column never
-- has to hold a multi-megabyte original.
-- ============================================================

ALTER TABLE employees
    ADD COLUMN photo_data LONGTEXT NULL AFTER pan_number;
