-- ============================================================
-- V107: Overtime Register - who did overtime, on which day, how many hours
--
-- The overtime_hours field added on employee_payroll_adjustments (V105) was a single
-- per-employee-per-month number, editable directly on the Payroll Run screen - it had no
-- day-wise detail at all (which day, whose overtime, who recorded it), and every edit silently
-- overwrote the previous value with no history. That's fine for "how many hours total this
-- month" but useless for "did employee X actually work overtime on the 14th, and who logged it".
--
-- employee_overtime_records is the actual source of truth from here on: one row per
-- employee+date (like attendance), so the month's total overtime hours becomes something
-- DERIVED (summed) from a real, auditable log, rather than a bare number someone typed once.
-- employee_payroll_adjustments.overtime_hours is left in place but no longer read by payroll
-- calculation (see PayrollRunService) - same "don't drop an unused column" precedent as the
-- legacy 'overtime' amount column before it.
-- ============================================================

CREATE TABLE employee_overtime_records (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id  BIGINT       NOT NULL,
    employee_id        BIGINT       NOT NULL,
    overtime_date      DATE         NOT NULL,
    hours              DECIMAL(4,2) NOT NULL,
    remarks            VARCHAR(255) NULL,
    marked_by          BIGINT       NULL,
    marked_by_role     VARCHAR(30)  NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_overtime_record_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT uq_overtime_employee_date UNIQUE (employee_id, overtime_date)
) ENGINE=InnoDB;

CREATE INDEX idx_overtime_records_lookup ON employee_overtime_records (client_company_id, employee_id, overtime_date);

-- New permissions - mirrors the ATTENDANCE_CORRECTION_REQUEST/REVIEW split (record vs review),
-- since "who can log overtime" and "who can just see the register" are naturally different
-- audiences (a Supervisor might log it; a Client Admin/HR just needs to see the total feeding
-- into payroll).
INSERT INTO permissions (name, description) VALUES
    ('OVERTIME_RECORD_MANAGE', 'Log and edit day-wise overtime hours for employees'),
    ('OVERTIME_RECORD_READ', 'View the overtime register');

-- Granted broadly to the same admin-tier roles that already handle manual attendance (matching
-- ADMIN_MANUAL_ATTENDANCE/SUPERVISOR_MANUAL_ATTENDANCE/HR_MANUAL_ATTENDANCE's audience) - the
-- OVERTIME_MANAGEMENT company feature (V106) remains the upper-level gate regardless of who
-- holds this permission.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE p.name IN ('OVERTIME_RECORD_MANAGE', 'OVERTIME_RECORD_READ')
  AND r.name IN ('CLIENT_ADMIN', 'ADMIN', 'HR_ADMIN', 'SITE_ADMIN', 'SITE_SUPERVISOR')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
