-- ============================================================
-- V14: New roles for the multi-tenant model
-- Existing roles (SUPER_ADMIN, ADMIN, MANAGER, USER, CLIENT) are untouched.
-- ============================================================

INSERT INTO roles (name, description) VALUES
    ('CLIENT_ADMIN', 'Administrator for a single Client Company tenant - manages that tenant''s employees, sub-clients, sites and assignments'),
    ('CLIENT_USER',  'Limited-access user scoped to a single Client Company tenant');
