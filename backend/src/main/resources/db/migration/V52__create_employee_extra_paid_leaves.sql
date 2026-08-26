-- ============================================================
-- V52: Paid Leave Management - extra/additional leave grant history
--
-- A one-time additional Paid Leave grant (e.g. 30 days Medical Leave),
-- kept completely separate from the monthly allocation (spec section 16).
-- Never overwritten - cancelling one sets status=CANCELLED but the row
-- (and its original created_at/created_by) is preserved for history
-- (spec section 6).
-- ============================================================

CREATE TABLE employee_extra_paid_leaves (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    employee_id         BIGINT        NOT NULL,
    leave_days          DECIMAL(6,2)  NOT NULL,
    reason              VARCHAR(20)   NOT NULL,
    start_date          DATE          NOT NULL,
    end_date            DATE,
    remark              VARCHAR(255),
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    CONSTRAINT fk_extra_paid_leave_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_extra_paid_leave_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_extra_paid_leave_employee ON employee_extra_paid_leaves (employee_id, start_date);
CREATE INDEX idx_extra_paid_leave_company ON employee_extra_paid_leaves (client_company_id);
