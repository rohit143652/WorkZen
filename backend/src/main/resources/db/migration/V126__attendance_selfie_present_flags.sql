-- ============================================================
-- V126: "has a selfie" flags on attendance
--
-- The selfie images (V114: check_in_selfie_data / check_out_selfie_data, base64, ~40-80 KB each) were mapped
-- onto the Attendance entity, so ANY query loading attendance rows - a month's muster, the monthly report,
-- a payroll run for every employee - read every image into memory (hundreds of MB for a few hundred employees).
-- The entity no longer maps the image columns (they stay in the table, untouched); these two cheap flags are
-- what the screens read instead ("show a view-photo button"), and the images are fetched one at a time on demand.
-- ============================================================
ALTER TABLE attendance
    ADD COLUMN check_in_selfie_present  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN check_out_selfie_present BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE attendance SET check_in_selfie_present  = TRUE WHERE check_in_selfie_data  IS NOT NULL AND check_in_selfie_data  <> '';
UPDATE attendance SET check_out_selfie_present = TRUE WHERE check_out_selfie_data IS NOT NULL AND check_out_selfie_data <> '';
