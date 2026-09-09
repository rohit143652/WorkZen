-- ============================================================
-- V106: OVERTIME_MANAGEMENT company feature
--
-- Same pattern as the Attendance feature codes (V102) - a company_feature row per tenant, Super
-- Admin controlled via the existing Manage Features screen (FeatureCode.CATALOG). Seeded FALSE
-- (not TRUE like the Attendance codes) for every EXISTING tenant deliberately: Overtime is a
-- brand-new capability with a real payroll-impacting calculation change (Net Pay now includes an
-- overtime line - see PayrollCalculationService), so it should be an explicit Super Admin
-- opt-IN per company, never silently on for existing customers who haven't asked for it.
-- ============================================================

INSERT INTO company_feature (client_company_id, feature_code, enabled)
SELECT c.id, 'OVERTIME_MANAGEMENT', FALSE
FROM client_companies c
WHERE NOT EXISTS (
    SELECT 1 FROM company_feature cf WHERE cf.client_company_id = c.id AND cf.feature_code = 'OVERTIME_MANAGEMENT'
);
