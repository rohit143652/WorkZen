-- ============================================================
-- V12: Employee tenant ownership + Employee <-> Site assignment history
-- ============================================================

ALTER TABLE employees
    ADD COLUMN client_company_id BIGINT AFTER id,
    ADD CONSTRAINT fk_employees_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE RESTRICT;

CREATE INDEX idx_employees_company_status ON employees (client_company_id, status);

-- Employee codes were globally unique (V6). For multi-tenant use they should
-- be unique per tenant instead (two different client companies may both use
-- "EMP001"). Replace the global constraint with a composite one. Existing
-- rows have client_company_id = NULL (pre-tenant "house" employees, if any);
-- MySQL unique indexes treat each NULL as distinct, so this remains safe.
ALTER TABLE employees DROP INDEX uq_employees_code;
ALTER TABLE employees ADD CONSTRAINT uq_employees_company_code UNIQUE (client_company_id, employee_code);

CREATE TABLE employee_site_assignments (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id    BIGINT        NOT NULL,
    employee_id          BIGINT        NOT NULL,
    site_id              BIGINT        NOT NULL,
    assignment_type      VARCHAR(30)   NOT NULL DEFAULT 'REGULAR',
    start_date           DATE          NOT NULL,
    end_date             DATE,
    is_primary           BOOLEAN       NOT NULL DEFAULT TRUE,
    status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    remarks               VARCHAR(255),
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            BIGINT,
    updated_by            BIGINT,
    CONSTRAINT fk_assignments_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_site FOREIGN KEY (site_id) REFERENCES sites (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_assignments_company_site ON employee_site_assignments (client_company_id, site_id);
CREATE INDEX idx_assignments_employee_status ON employee_site_assignments (employee_id, status);
CREATE INDEX idx_assignments_site_status ON employee_site_assignments (site_id, status);
CREATE INDEX idx_assignments_dates ON employee_site_assignments (start_date, end_date);
