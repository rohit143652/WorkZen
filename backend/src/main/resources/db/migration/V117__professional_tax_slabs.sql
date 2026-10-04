-- ============================================================
-- V117: Professional Tax slabs
--
-- AUDIT FINDING: payroll_settings.professional_tax was always a single flat monthly amount for
-- the whole company - correct for the SP College reference file (PT=0 throughout), but Indian
-- Professional Tax is usually slab-based (different amounts at different gross-salary bands,
-- varying by state). No slab structure existed anywhere in the project to reuse (confirmed by
-- inspection before adding this), so this is new, not a duplicate of something already there.
--
-- payroll_settings keeps its existing flat professional_tax column - that stays the behavior
-- when pt_calculation_mode = 'FLAT' (the default, so every existing company's PT is completely
-- unaffected). pt_calculation_mode = 'SLAB' instead looks up the matching row here by gross
-- salary falling within [min_salary, max_salary] (max_salary NULL = unbounded top slab).
-- ============================================================

ALTER TABLE payroll_settings
    ADD COLUMN pt_calculation_mode VARCHAR(20) NOT NULL DEFAULT 'FLAT' AFTER pt_enabled;

CREATE TABLE professional_tax_slabs (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id BIGINT         NOT NULL,
    state             VARCHAR(100)   NULL,  -- NULL = applies regardless of state (most companies operate in one state and won't need this distinction at all)
    min_salary        DECIMAL(12,2)  NOT NULL,
    max_salary        DECIMAL(12,2)  NULL,  -- NULL = unbounded (the top slab)
    pt_amount         DECIMAL(10,2)  NOT NULL,
    effective_from    DATE           NOT NULL,
    effective_to      DATE           NULL,  -- NULL = open-ended (still the latest slab table)
    status            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT         NULL,
    CONSTRAINT fk_pt_slab_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
);

CREATE INDEX idx_pt_slab_lookup ON professional_tax_slabs (client_company_id, status, effective_from, effective_to);
