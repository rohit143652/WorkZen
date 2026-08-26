-- ============================================================
-- V2: Default roles
-- ============================================================

INSERT INTO roles (name, description) VALUES
    ('SUPER_ADMIN', 'Full system access via database-driven permissions'),
    ('ADMIN',       'User management and dashboard access'),
    ('MANAGER',     'Dashboard access and assigned management functionality'),
    ('USER',        'Standard dashboard access'),
    ('CLIENT',      'Client-related functionality only');
