-- ============================================================
-- V88: GPS Geofencing for Attendance
--
-- Nullable/optional - a site with no latitude/longitude set has NO geofence restriction at all
-- (attendance marking works exactly as before). Only once BOTH latitude and longitude are set
-- for a site does AttendanceService start checking the marking employee's device location
-- against it, within geofence_radius_meters (defaults to a sensible 200m if left blank).
-- ============================================================

ALTER TABLE sites
    ADD COLUMN latitude              DECIMAL(10, 7) NULL AFTER site_contact_number,
    ADD COLUMN longitude             DECIMAL(10, 7) NULL AFTER latitude,
    ADD COLUMN geofence_radius_meters INT NULL AFTER longitude;

-- The actual device location captured at the moment attendance was marked - kept purely as a
-- record/audit trail (e.g. to review a disputed attendance mark later), never used to compute
-- anything after the fact.
ALTER TABLE attendance
    ADD COLUMN marked_latitude  DECIMAL(10, 7) NULL AFTER remarks,
    ADD COLUMN marked_longitude DECIMAL(10, 7) NULL AFTER marked_latitude;
