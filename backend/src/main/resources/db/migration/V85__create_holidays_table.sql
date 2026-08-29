-- ============================================================
-- V85: Company Holiday Calendar
--
-- One row per (tenant, date range) - a public/company holiday, optionally spanning several
-- consecutive days (e.g. a 3-day festival). Creating one auto-marks every currently active,
-- site-assigned employee PRESENT for every date in that range (see HolidayService.create()), so
-- it flows straight into Payable Days the same way any other PRESENT attendance record already
-- does (PayrollInputResolver counts PRESENT attendance rows directly - no separate "holiday"
-- concept needed there at all).
-- ============================================================

CREATE TABLE holidays (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT       NOT NULL,
    start_date          DATE         NOT NULL,
    end_date            DATE         NOT NULL,
    name                VARCHAR(150) NOT NULL,
    description         VARCHAR(500),
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_holiday_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT chk_holiday_date_order CHECK (end_date >= start_date)
);

-- No DB-level uniqueness on the date range itself - two ranges overlapping is checked in
-- application code (HolidayService), which can give a much clearer error message than a
-- constraint violation ever could.
CREATE INDEX idx_holiday_company_dates ON holidays (client_company_id, start_date, end_date);

INSERT INTO permissions (name, description) VALUES
    ('HOLIDAY_READ',   'View the company holiday calendar'),
    ('HOLIDAY_CREATE', 'Add a company holiday (auto-marks all active employees Present for every date in its range)'),
    ('HOLIDAY_DELETE', 'Remove a company holiday');

-- CLIENT_ADMIN only, matching the requirement that this calendar is Client-Admin-managed.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN'
  AND p.name IN ('HOLIDAY_READ', 'HOLIDAY_CREATE', 'HOLIDAY_DELETE');
