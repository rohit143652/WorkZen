-- ============================================================
-- V104: Statutory IDs (UAN, PF Member ID, ESIC) and bank details on Employee
--
-- All nullable and genuinely OPTIONAL at the application layer too (unlike Aadhar/PAN in V87,
-- which are nullable at the DB level but required going forward) - these are informational
-- fields an HR admin can fill in when convenient, not a blocker for adding a new employee.
--
-- No uniqueness constraints added, deliberately: these are frequently left blank for a while
-- (e.g. UAN is only issued once PF registration completes, sometimes weeks after joining), and
-- forcing uniqueness on a field most rows won't have yet adds friction for no real benefit here
-- (unlike Aadhar/PAN, which are near-universally available at the time of hiring).
-- ============================================================

ALTER TABLE employees
    ADD COLUMN uan_number                VARCHAR(20)  NULL AFTER pan_number,
    ADD COLUMN pf_member_id              VARCHAR(30)  NULL AFTER uan_number,
    ADD COLUMN esic_number               VARCHAR(20)  NULL AFTER pf_member_id,
    ADD COLUMN bank_account_holder_name  VARCHAR(150) NULL AFTER esic_number,
    ADD COLUMN bank_account_number       VARCHAR(30)  NULL AFTER bank_account_holder_name,
    ADD COLUMN bank_ifsc_code            VARCHAR(11)  NULL AFTER bank_account_number,
    ADD COLUMN bank_name                 VARCHAR(150) NULL AFTER bank_ifsc_code,
    ADD COLUMN bank_branch               VARCHAR(150) NULL AFTER bank_name;
