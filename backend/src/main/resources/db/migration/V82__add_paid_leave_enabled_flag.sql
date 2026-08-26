-- ============================================================
-- V82: Master enable/disable switch for Paid Leave
--
-- Some clients don't want to grant any monthly paid leave at all - rather
-- than admins having to remember "set Monthly Paid Leave to 0" (easy to
-- forget, and reads as a number to configure rather than a deliberate
-- policy decision), this adds an explicit "Paid Leave Enabled" switch,
-- effective-dated exactly like every other setting on this table.
--
-- When disabled for a given month, LeavePolicyResolver treats that
-- month's monthly entitlement as 0 - already-carried-forward balance from
-- an earlier ACTIVE period is untouched and can still be used, but no NEW
-- leave accrues while disabled.
-- ============================================================

ALTER TABLE paid_leave_configurations
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
