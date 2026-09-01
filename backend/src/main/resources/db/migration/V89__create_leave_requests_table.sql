-- ============================================================
-- V89: Leave Request Workflow
--
-- Two ways a leave request comes into existence:
--   1. Self-service (employee has login + LEAVE_REQUEST_SELF_CREATE) - starts PENDING, needs an
--      admin/supervisor to approve or reject it.
--   2. Admin/supervisor direct-add (LEAVE_REQUEST_MANAGE) - for an employee without login, or
--      any employee really - goes straight to APPROVED, no separate review step, since the
--      person adding it already IS the approver.
--
-- Approval (either path) immediately marks Attendance ON_LEAVE for every date in the range (see
-- AttendanceService.markOnLeaveForRange()) - Payroll already treats ON_LEAVE attendance rows as
-- paid/unpaid leave automatically based on the employee's Paid Leave balance (see
-- PayrollInputResolver), so nothing else needs to change for this to flow into salary
-- calculation correctly.
-- ============================================================

CREATE TABLE leave_requests (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT       NOT NULL,
    employee_id         BIGINT       NOT NULL,
    start_date          DATE         NOT NULL,
    end_date            DATE         NOT NULL,
    reason              VARCHAR(500),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    self_requested      BOOLEAN      NOT NULL DEFAULT FALSE,

    reviewed_by         BIGINT,
    reviewed_at         TIMESTAMP NULL,
    review_note         VARCHAR(500),

    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_leave_request_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_request_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT chk_leave_request_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_leave_request_company_employee ON leave_requests (client_company_id, employee_id);

INSERT INTO permissions (name, description) VALUES
    ('LEAVE_REQUEST_READ',        'View leave requests'),
    ('LEAVE_REQUEST_SELF_CREATE', 'Apply for one''s own leave (self-service, requires an admin/supervisor to approve)'),
    ('LEAVE_REQUEST_MANAGE',      'Approve/reject leave requests, or add approved leave directly on an employee''s behalf');

-- CLIENT_ADMIN gets read + manage by default, matching every other admin-side workflow in this
-- app. LEAVE_REQUEST_SELF_CREATE is deliberately granted to nobody by default - same pattern as
-- PAYSLIP_SELF_VIEW (V84) - an admin decides which roles get self-service leave requests.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN'
  AND p.name IN ('LEAVE_REQUEST_READ', 'LEAVE_REQUEST_MANAGE');
