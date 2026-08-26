-- ============================================================
-- V46: Attendance settings (paid-leave policy)
--
-- One row per tenant, created lazily on first save - see
-- AttendanceSettingsService.getEntityOrDefault(). No default rows are
-- seeded here; a tenant with no row simply uses the hardcoded defaults
-- (paid, 2 days/month) until it explicitly saves a preference.
-- ============================================================

CREATE TABLE attendance_settings (
    client_company_id          BIGINT        NOT NULL PRIMARY KEY,
    paid_leave_enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    paid_leave_days_per_month  INT           NOT NULL DEFAULT 2,
    updated_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by                 BIGINT,
    CONSTRAINT fk_attendance_settings_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;
