-- ============================================================
-- V37: Salary Components (database-driven, not hardcoded)
--
-- A component is one line item on a payslip: BASIC, HRA, CONVEYANCE, PF,
-- ESI, etc. component_type says which side of the payslip it belongs on;
-- calculation_type says how its amount is derived within a Salary
-- Structure. FORMULA calculation is deliberately NOT supported (no unsafe
-- dynamic expression evaluation) - only FIXED, PERCENTAGE_OF_BASIC,
-- PERCENTAGE_OF_GROSS, and MANUAL are implemented; PER_DAY/PER_HOUR belong
-- to Payroll Processing (attendance-driven), not the static structure
-- definition, and will be handled there when that module is built.
-- ============================================================

CREATE TABLE salary_components (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    component_code      VARCHAR(50)   NOT NULL,
    component_name      VARCHAR(150)  NOT NULL,
    component_type      VARCHAR(30)   NOT NULL,
    calculation_type     VARCHAR(30)   NOT NULL,
    value                DECIMAL(12,2),
    percentage           DECIMAL(5,2),
    is_taxable           BOOLEAN       NOT NULL DEFAULT TRUE,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order        INT           NOT NULL DEFAULT 0,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT,
    CONSTRAINT fk_salary_components_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_salary_components_company_code UNIQUE (client_company_id, component_code)
) ENGINE=InnoDB;

CREATE INDEX idx_salary_components_company_active ON salary_components (client_company_id, is_active);
