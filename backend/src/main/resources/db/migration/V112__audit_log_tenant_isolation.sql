-- ============================================================
-- V112: Tenant isolation for audit_logs
--
-- PHASE 1 SECURITY AUDIT FINDING: AuditService.findAll() had NO tenant filtering at all - it
-- returned every audit entry across every client company with no scoping whatsoever. This was
-- not actively exploited by the CURRENT default role grants (AUDIT_LOG_READ is only granted to
-- SUPER_ADMIN and the legacy internal ADMIN role today, never to a tenant's own CLIENT_ADMIN),
-- but the service itself was structurally unsafe: the moment anyone granted AUDIT_LOG_READ to a
-- tenant-scoped role (via the Role Management UI, which allows exactly this), that tenant's
-- admin would immediately see every OTHER company's audit trail - usernames, IP addresses, and
-- descriptions of what happened in companies they have no relationship to. Fixed at the root
-- (the service now always filters, never trusts that only "safe" roles will ever hold the
-- permission) rather than only fixing today's role grants.
-- ============================================================

ALTER TABLE audit_logs
    ADD COLUMN client_company_id BIGINT NULL AFTER user_id;

-- Backfill every existing row from the user who performed the action. Rows with no user_id
-- (system-triggered events, e.g. the subscription auto-expiry job) or a user_id whose account no
-- longer resolves to a company (already-deleted user, or a genuinely platform-level SUPER_ADMIN
-- action) are left NULL - see AuditService.findAll()'s comment for how NULL is treated on read.
UPDATE audit_logs a
JOIN users u ON u.id = a.user_id
SET a.client_company_id = u.client_company_id
WHERE a.client_company_id IS NULL;

CREATE INDEX idx_audit_logs_company ON audit_logs (client_company_id, created_at);
