-- ============================================================
-- V39: Employee <-> Salary Structure assignment history
--
-- Mirrors the same "never overwrite, always end the old row and start a
-- new one" pattern already used for employee_site_assignments. An
-- employee's current structure is the row with status='ACTIVE' and
-- effective_to IS NULL; assigning a new structure ends the previous one
-- (effective_to = day before the new structure's effective_from) rather
-- than deleting or mutating it, so payroll for a past period can always
-- resolve "which structure applied on that date" correctly.
-- ============================================================

CREATE TABLE employee_salary_structures (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id     BIGINT        NOT NULL,
    employee_id           BIGINT        NOT NULL,
    salary_structure_id   BIGINT        NOT NULL,
    effective_from        DATE          NOT NULL,
    effective_to          DATE,
    status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            BIGINT,
    CONSTRAINT fk_ess_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_ess_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_ess_structure FOREIGN KEY (salary_structure_id) REFERENCES salary_structures (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_ess_employee_status ON employee_salary_structures (employee_id, status);
CREATE INDEX idx_ess_company ON employee_salary_structures (client_company_id);
CREATE INDEX idx_ess_dates ON employee_salary_structures (effective_from, effective_to);
