-- ============================================================
-- V4: Default SUPER_ADMIN user
-- Username: super_admin
-- Password: admin123  (BCrypt hash below, strength 12)
--
-- IMPORTANT: this default password MUST be changed immediately
-- after first login in any real deployment.
-- ============================================================

INSERT INTO users (username, email, password, first_name, last_name, is_active, is_locked, password_changed_at)
VALUES (
    'super_admin',
    'super_admin@workforce.local',
    '$2b$12$Vg5B2vmiI7t1Mr31z1DrhufpruExhhDE3GZCvORNyh0IZmxfeWIvS',
    'Super',
    'Admin',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'super_admin' AND r.name = 'SUPER_ADMIN';
