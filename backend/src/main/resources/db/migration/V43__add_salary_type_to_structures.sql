-- ============================================================
-- V43: Salary Type on Salary Structures (spec sections 8, 13-16)
--
-- MONTHLY/DAILY/HOURLY/CONTRACT is a property of the structure itself
-- (a "Housekeeping Daily Wager" template is always daily-rated), not of
-- an individual employee's assignment. daily_rate/hourly_rate are plain
-- reference values for future Attendance/Payroll to multiply against;
-- they are NOT calculated here (no attendance data available yet) and are
-- independent of the salary_structure_components rows, which still drive
-- gross_earnings/net_salary for MONTHLY/CONTRACT structures.
-- ============================================================

ALTER TABLE salary_structures
    ADD COLUMN salary_type VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' AFTER structure_name,
    ADD COLUMN daily_rate  DECIMAL(12,2) AFTER description,
    ADD COLUMN hourly_rate DECIMAL(12,2) AFTER daily_rate;

CREATE INDEX idx_salary_structures_company_type ON salary_structures (client_company_id, salary_type);
