-- ============================================================
-- V23: Tenant-scoped custom roles
--
-- roles.client_company_id NULL = "system"/"house" role (SUPER_ADMIN, ADMIN,
-- MANAGER, USER, CLIENT, CLIENT_ADMIN, CLIENT_USER - all existing rows stay
-- NULL, untouched). Non-NULL = a custom role created by that tenant's own
-- Client Admin (e.g. "HR_ADMIN", "SITE_SUPERVISOR").
--
-- The old global UNIQUE(name) is replaced with UNIQUE(client_company_id, name).
-- MySQL/InnoDB treats each NULL in a unique index as distinct from every
-- other NULL, so this constraint alone does NOT stop two different system
-- (NULL-scope) roles from sharing a name at the database level - but system
-- roles are only ever created by trusted Flyway migrations or by SUPER_ADMIN
-- through RoleService, which already checks
-- existsByClientCompanyIdIsNullAndNameIgnoreCase() before insert, so this is
-- an acceptable trade-off. What the constraint DOES fully guarantee at the
-- database level is what actually matters here: each tenant's own custom
-- role names are unique within that tenant.
--
-- (An earlier version of this migration used a STORED generated column to
-- get airtight global+per-tenant uniqueness in one index. That hit a known
-- MySQL/InnoDB limitation - Error 1215 "Cannot add foreign key constraint" -
-- when adding a STORED generated column to a table that already has a
-- foreign key, even though the statement doesn't touch any FK. Simpler and
-- more portable to drop that approach entirely.)
-- ============================================================

ALTER TABLE roles
    ADD COLUMN client_company_id BIGINT NULL AFTER id;

ALTER TABLE roles
    ADD CONSTRAINT fk_roles_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE;

ALTER TABLE roles
    DROP INDEX uq_roles_name;

ALTER TABLE roles
    ADD CONSTRAINT uq_roles_company_name UNIQUE (client_company_id, name);

CREATE INDEX idx_roles_company ON roles (client_company_id);
