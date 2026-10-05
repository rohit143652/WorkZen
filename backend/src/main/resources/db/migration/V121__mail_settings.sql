-- ============================================================
-- V121: Super-Admin-managed outgoing email (SMTP) settings
--
-- Until now the SMTP account lived only in environment variables (MAIL_*), so changing the sender
-- or its password meant editing the server's environment and restarting. This table lets a Super
-- Admin manage it from the app instead; the environment variables remain the fallback when no row
-- exists (or the row is disabled), so existing deployments keep working untouched.
--
-- password_encrypted holds AES-256-GCM ciphertext - NEVER the plain password. An SMTP password has
-- to be recoverable (the server must send it to the mail provider), so it can't be hashed; it is
-- encrypted with a key that lives OUTSIDE the database (APP_ENCRYPTION_KEY), so a leaked database
-- dump or backup on its own does not reveal the mail account.
--
-- singleton_key + its UNIQUE constraint guarantee there is at most ONE row: this is the platform's
-- single outgoing mail account, not a per-company list.
-- ============================================================
CREATE TABLE mail_settings (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    singleton_key      TINYINT       NOT NULL DEFAULT 1,
    host               VARCHAR(255)  NOT NULL,
    port               INT           NOT NULL,
    username           VARCHAR(255)  NOT NULL,
    password_encrypted VARCHAR(1024) NOT NULL,
    from_address       VARCHAR(255)  NOT NULL,
    enabled            BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_by         BIGINT        NULL,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_mail_settings_singleton UNIQUE (singleton_key)
) ENGINE=InnoDB;

-- Platform-level setting: only the global SUPER_ADMIN role gets it. A company's own admins must
-- never be able to read or replace the platform's mail account.
INSERT INTO permissions (name, description) VALUES
    ('MAIL_SETTINGS_MANAGE', 'View, change, test and delete the platform outgoing email (SMTP) settings - Super Admin only');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE p.name = 'MAIL_SETTINGS_MANAGE'
  AND r.name = 'SUPER_ADMIN'
  AND r.client_company_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
