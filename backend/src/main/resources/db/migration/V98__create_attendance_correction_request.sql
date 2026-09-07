-- ============================================================
-- V98: Attendance correction requests
--
-- An employee's request to fix their own attendance record for one date -
-- never applies directly. Approving one calls back into the same
-- AttendanceService/AttendanceRulesEngine used for check-in/check-out, so
-- the resulting record is computed exactly the same way a normal
-- check-in/check-out would be, and the approval itself is logged through
-- the existing shared AuditService (no separate audit table needed here,
-- consistent with how every other module in this codebase logs changes).
-- ============================================================

CREATE TABLE attendance_correction_request (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id     BIGINT       NOT NULL,
    employee_id           BIGINT       NOT NULL,
    attendance_date       DATE         NOT NULL,
    request_type          VARCHAR(30)  NOT NULL,
    requested_check_in    DATETIME     NULL,
    requested_check_out   DATETIME     NULL,
    reason                VARCHAR(500) NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by           BIGINT       NULL,
    reviewed_at           DATETIME     NULL,
    review_remarks        VARCHAR(500) NULL,
    created_by            BIGINT       NOT NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_att_correction_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_att_correction_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_att_correction_company_status ON attendance_correction_request (client_company_id, status);
CREATE INDEX idx_att_correction_employee ON attendance_correction_request (employee_id, attendance_date);
