-- ============================================================
-- V124: Additional permissions for an individual user, on top of their role
--
-- Until now a user's permissions came ONLY from their role(s), so giving one person a single extra
-- ability meant inventing a whole new role for them. This table lets an admin grant specific
-- permissions to ONE user in addition to whatever their role already gives. It is strictly additive:
-- nothing here can remove a permission the role grants.
--
-- The effective permissions of a user are therefore: (permissions of their roles) UNION (rows here).
-- That union is computed in exactly one place (CustomUserPrincipal), which every authorization check
-- and the permission list sent to the browser both read from.
--
-- Deleting a user or a permission removes its rows here automatically (ON DELETE CASCADE).
-- Who granted what is recorded in the audit log, not in this table.
-- ============================================================
CREATE TABLE user_permissions (
    user_id       BIGINT   NOT NULL,
    permission_id BIGINT   NOT NULL,
    granted_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_user_permissions_user       FOREIGN KEY (user_id)       REFERENCES users (id)       ON DELETE CASCADE,
    CONSTRAINT fk_user_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Admin-tier, same footing as assigning a role. Deliberately NOT given to HR_ADMIN or any custom role:
-- it is a way of handing out access, so it stays with the company's top admins unless a Client Admin
-- chooses to delegate it. (Even then nobody can hand out a permission they do not hold themselves.)
INSERT INTO permissions (name, description) VALUES
    ('USER_PERMISSION_MANAGE', 'Give an individual user additional permissions on top of their role (limited to permissions you hold yourself)');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE p.name = 'USER_PERMISSION_MANAGE'
  AND r.name IN ('SUPER_ADMIN', 'CLIENT_ADMIN', 'ADMIN')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
