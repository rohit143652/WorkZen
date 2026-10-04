-- ============================================================
-- V118: Scope user email uniqueness to per-company, not global
--
-- AUDIT FINDING: uq_users_email (added in V1, before multi-tenancy existed) made a user's email
-- unique across the ENTIRE platform - meaning two completely unrelated client companies could
-- never both have an employee login with the same email address, even by pure coincidence. This
-- is the wrong scope for a multi-tenant system: `username` genuinely needs to stay globally
-- unique (login resolves a user by username alone, before the system knows which tenant they
-- belong to), but `email` is just contact/identity data with no such requirement - confirmed by
-- checking every findByEmail/existsByEmail caller in the codebase: all of them are plain
-- duplicate-check validation at creation/update time, none of them depend on email being a
-- platform-wide unique lookup key the way username is.
--
-- NULL client_company_id (SUPER_ADMIN / other "house" users with no single owning company) is
-- deliberately left out of real enforcement here: MySQL treats every NULL in a unique index as
-- distinct from every other NULL, so multiple house accounts could in principle share an email
-- without being blocked - acceptable, since house accounts are few, manually managed, and this
-- is strictly no worse than before (previously ALL users were blocked from sharing an email
-- regardless of tenant, which was the actual problem being fixed).
-- ============================================================

ALTER TABLE users DROP INDEX uq_users_email;

ALTER TABLE users ADD CONSTRAINT uq_users_company_email UNIQUE (client_company_id, email);
