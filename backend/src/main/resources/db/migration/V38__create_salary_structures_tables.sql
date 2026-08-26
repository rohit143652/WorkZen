-- ============================================================
-- V38: Salary Structures + their component line items
--
-- A Salary Structure is a named, reusable payroll template (e.g.
-- "Housekeeping Staff Grade A") built from a set of Salary Components,
-- each with its own calculation type/amount/percentage WITHIN this
-- structure (salary_structure_components.calculation_type/amount/
-- percentage override the component's own defaults, so the same "HRA"
-- component can be 3000 fixed in one structure and 10% of basic in
-- another).
-- ============================================================

CREATE TABLE salary_structures (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    structure_code       VARCHAR(50)   NOT NULL,
    structure_name       VARCHAR(150)  NOT NULL,
    description          VARCHAR(255),
    effective_from       DATE          NOT NULL,
    effective_to         DATE,
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT,
    CONSTRAINT fk_salary_structures_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_salary_structures_company_code UNIQUE (client_company_id, structure_code)
) ENGINE=InnoDB;

CREATE INDEX idx_salary_structures_company_status ON salary_structures (client_company_id, status);

CREATE TABLE salary_structure_components (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_structure_id   BIGINT        NOT NULL,
    salary_component_id   BIGINT        NOT NULL,
    calculation_type       VARCHAR(30)   NOT NULL,
    amount                 DECIMAL(12,2),
    percentage              DECIMAL(5,2),
    is_active               BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order           INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_ssc_structure FOREIGN KEY (salary_structure_id) REFERENCES salary_structures (id) ON DELETE CASCADE,
    CONSTRAINT fk_ssc_component FOREIGN KEY (salary_component_id) REFERENCES salary_components (id) ON DELETE RESTRICT,
    CONSTRAINT uq_ssc_structure_component UNIQUE (salary_structure_id, salary_component_id)
) ENGINE=InnoDB;

CREATE INDEX idx_ssc_structure ON salary_structure_components (salary_structure_id);
