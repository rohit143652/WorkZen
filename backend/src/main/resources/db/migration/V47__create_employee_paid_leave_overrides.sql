-- ============================================================
-- V47: Per-employee paid-leave overrides
--
-- Lets an admin give a specific employee more (or fewer, or zero) paid
-- leave days than the tenant default in attendance_settings - e.g. someone
-- on approved paid medical leave needing 3 paid days this month instead of
-- the usual 2. Both columns are independently nullable: a null value means
-- "inherit the tenant default for this field" (see AttendanceSettingsService).
-- ============================================================

CREATE TABLE employee_paid_leave_overrides (
    employee_id                BIGINT        NOT NULL PRIMARY KEY,
    client_company_id          BIGINT        NOT NULL,
    paid_leave_enabled         BOOLEAN,
    paid_leave_days_per_month  INT,
    updated_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by                 BIGINT,
    CONSTRAINT fk_paid_leave_override_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_paid_leave_override_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_paid_leave_override_company ON employee_paid_leave_overrides (client_company_id);
