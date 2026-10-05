-- ============================================================
-- V120: Track whether each onboarding invitation EMAIL actually went out
--
-- The invitation email used to be sent synchronously inside the "save employee" request, which
-- made saving slow (an SMTP round-trip per save) and held the database transaction open for the
-- whole time. It is now sent in the background AFTER the employee is saved and committed - so the
-- outcome has to be recorded somewhere the admin can see it later, instead of only in that one
-- request's response:
--   PENDING = queued / being sent right now
--   SENT    = the mail server accepted it
--   FAILED  = the send failed - the admin should use "Resend Invitation"
--
-- Rows that already exist were sent the old way and their outcome was never recorded; SENT is
-- the right neutral default (marking them FAILED would show a false alarm on every old employee).
-- ============================================================

ALTER TABLE employee_onboarding_invitations
    ADD COLUMN email_status  VARCHAR(20) NOT NULL DEFAULT 'SENT',
    ADD COLUMN email_sent_at DATETIME    NULL;
