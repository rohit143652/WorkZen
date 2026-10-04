-- ============================================================
-- V119: Scope employee email uniqueness to per-company, not global
--
-- Same fix as V118 (users.email), found on a second table while addressing a related request -
-- employees.uq_employees_email (added in V6, before multi-tenancy was fully built out) made an
-- employee's email unique across the ENTIRE platform, not just within their own company. Two
-- unrelated client companies should never be blocked from both having an employee with the same
-- email by pure coincidence.
-- ============================================================

ALTER TABLE employees DROP INDEX uq_employees_email;

ALTER TABLE employees ADD CONSTRAINT uq_employees_company_email UNIQUE (client_company_id, email);
