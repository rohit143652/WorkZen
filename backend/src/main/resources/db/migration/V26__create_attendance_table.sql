-- ============================================================
-- V26: Attendance
--
-- One row per employee per calendar day. UNIQUE(client_company_id,
-- employee_id, attendance_date) is the database-level backstop for the
-- "mark once" business rule - AttendanceService.mark() also checks this
-- explicitly first for a clean error message, but the constraint means
-- even a race between two concurrent requests can't create a duplicate.
-- ============================================================

CREATE TABLE attendance (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    employee_id         BIGINT        NOT NULL,
    site_id             BIGINT        NOT NULL,
    attendance_date     DATE          NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    remarks             VARCHAR(255),
    marked_by           BIGINT,
    updated_by          BIGINT,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_site FOREIGN KEY (site_id) REFERENCES sites (id) ON DELETE CASCADE,
    CONSTRAINT uq_attendance_employee_date UNIQUE (client_company_id, employee_id, attendance_date)
) ENGINE=InnoDB;

CREATE INDEX idx_attendance_company_date ON attendance (client_company_id, attendance_date);
CREATE INDEX idx_attendance_site_date ON attendance (site_id, attendance_date);
CREATE INDEX idx_attendance_employee_date ON attendance (employee_id, attendance_date);
