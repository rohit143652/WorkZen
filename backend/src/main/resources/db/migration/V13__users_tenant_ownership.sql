-- ============================================================
-- V13: Tenant ownership on users
--
-- SUPER_ADMIN and any other "house" user: client_company_id IS NULL.
-- CLIENT_ADMIN / employee-linked logins: client_company_id = their tenant.
-- This is set exclusively by the backend (from TenantContextService or
-- from the owning Employee's client_company_id) - it is never accepted
-- from a request body.
-- ============================================================

ALTER TABLE users
    ADD COLUMN client_company_id BIGINT AFTER id,
    ADD CONSTRAINT fk_users_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE RESTRICT;

CREATE INDEX idx_users_company ON users (client_company_id);
