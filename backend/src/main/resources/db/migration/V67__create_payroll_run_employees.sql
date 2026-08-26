-- ============================================================
-- V67: Persisted Payroll Run Employee snapshots (architecture refactor Phase 2)
--
-- One row per employee per PayrollRun - a permanent monthly snapshot that
-- never changes just because attendance/leave/salary-structure/PF-ESI-PT
-- settings change afterward. Only re-running calculate() while the parent
-- run is still DRAFT/CALCULATED ever overwrites a row here (enforced at
-- the service layer, not the DB, since MySQL can't express "immutable
-- once parent status is X" as a constraint).
-- ============================================================

CREATE TABLE payroll_run_employees (
    id                                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    payroll_run_id                         BIGINT        NOT NULL,
    employee_id                            BIGINT        NOT NULL,
    employee_code                          VARCHAR(50)   NOT NULL,
    employee_name                          VARCHAR(200)  NOT NULL,
    department                             VARCHAR(150),
    designation                            VARCHAR(150),
    site_name                              VARCHAR(150),
    salary_structure_name                  VARCHAR(150),
    salary_type                            VARCHAR(20),

    total_calendar_days                     INT           NOT NULL,
    present_days                            INT           NOT NULL,
    half_days                               INT           NOT NULL,
    on_leave_days                           INT           NOT NULL,
    absent_days                             INT           NOT NULL,
    paid_leave_days                         DECIMAL(6,2)  NOT NULL DEFAULT 0,
    unpaid_leave_days                       DECIMAL(6,2)  NOT NULL DEFAULT 0,
    payable_days                            DECIMAL(6,2)  NOT NULL DEFAULT 0,
    leave_balance_closing                   DECIMAL(6,2),

    basic_salary                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    da                                      DECIMAL(12,2) NOT NULL DEFAULT 0,
    gross_salary                            DECIMAL(12,2) NOT NULL DEFAULT 0,

    allowance                               DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_earnings                          DECIMAL(12,2) NOT NULL DEFAULT 0,

    epf_employee                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    epf_employer                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    esi_employee                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    esi_employer                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    professional_tax                        DECIMAL(12,2) NOT NULL DEFAULT 0,
    other_manual_deduction                  DECIMAL(12,2) NOT NULL DEFAULT 0,
    advance_recovery                        DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_deductions                        DECIMAL(12,2) NOT NULL DEFAULT 0,

    advance_outstanding_before_recovery      DECIMAL(12,2) NOT NULL DEFAULT 0,
    advance_outstanding_after_recovery       DECIMAL(12,2) NOT NULL DEFAULT 0,

    total_salary_ctc                        DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_pay                                 DECIMAL(12,2) NOT NULL DEFAULT 0,
    note                                    VARCHAR(500),

    created_at                              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payroll_run_employee_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_payroll_run_employee_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT uq_payroll_run_employee UNIQUE (payroll_run_id, employee_id)
) ENGINE=InnoDB;

CREATE INDEX idx_payroll_run_employee_employee ON payroll_run_employees (employee_id);
