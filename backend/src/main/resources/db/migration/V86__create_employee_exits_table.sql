-- ============================================================
-- V86: Exit Management (Full & Final Settlement)
--
-- One row per employee exit process: resignation recorded first (status INITIATED, employee
-- still ACTIVE and working out their notice period), then settled later on/after their last
-- working day (status SETTLED, which is what actually deactivates the Employee - see
-- ExitService.settle()). The settlement figures are computed once at settle() time and stored
-- here as a permanent record - never recomputed afterward, same "once persisted, immutable"
-- convention as Payroll Run figures elsewhere in this app.
-- ============================================================

CREATE TABLE employee_exits (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id               BIGINT       NOT NULL,
    employee_id                     BIGINT       NOT NULL,
    resignation_date                DATE         NOT NULL,
    last_working_day                DATE         NOT NULL,
    reason                          VARCHAR(500),
    status                          VARCHAR(20)  NOT NULL DEFAULT 'INITIATED',

    -- Populated only once settle() runs - NULL while status = INITIATED.
    prorated_salary                 DECIMAL(12,2),
    outstanding_advance_deduction    DECIMAL(12,2),
    net_settlement_amount           DECIMAL(12,2),
    settled_at                      TIMESTAMP NULL,
    settled_by                      BIGINT,

    created_by                      BIGINT,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_exit_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_exit_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT chk_exit_dates CHECK (last_working_day >= resignation_date),
    CONSTRAINT chk_exit_status CHECK (status IN ('INITIATED', 'SETTLED'))
);

CREATE INDEX idx_exit_company_employee ON employee_exits (client_company_id, employee_id);

INSERT INTO permissions (name, description) VALUES
    ('EMPLOYEE_EXIT_READ',   'View employee exit / resignation records'),
    ('EMPLOYEE_EXIT_CREATE', 'Record an employee resignation (notice period start)'),
    ('EMPLOYEE_EXIT_SETTLE', 'Process an employee''s Full & Final Settlement (deactivates the employee)');

-- CLIENT_ADMIN only, matching every other sensitive employee-lifecycle action in this app.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN'
  AND p.name IN ('EMPLOYEE_EXIT_READ', 'EMPLOYEE_EXIT_CREATE', 'EMPLOYEE_EXIT_SETTLE');
