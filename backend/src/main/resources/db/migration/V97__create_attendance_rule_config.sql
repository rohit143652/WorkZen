-- ============================================================
-- V97: Per-tenant configurable attendance rules
--
-- One row per client company (UNIQUE constraint) instead of hardcoding
-- office hours/grace periods/thresholds anywhere in Java, matching how
-- other per-tenant configuration already works in this codebase (e.g.
-- paid_leave_config). A sensible default row is seeded for every EXISTING
-- tenant so nothing is left unconfigured after this migration; any tenant
-- created after this point gets a default row created the first time
-- AttendanceRuleConfigService.getForCurrentTenant() is called (see that
-- service - it lazily creates-on-read rather than needing a hook on tenant
-- creation).
--
-- weekly_off_days is a comma-separated list of java.time.DayOfWeek names
-- (e.g. "SATURDAY,SUNDAY") rather than a bitmask - keeps it trivially
-- readable/editable directly in the database if ever needed, and parsing
-- a comma list in Java is one line.
-- ============================================================

CREATE TABLE attendance_rule_config (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id        BIGINT       NOT NULL,
    office_start_time         TIME         NOT NULL DEFAULT '09:30:00',
    office_end_time           TIME         NOT NULL DEFAULT '18:30:00',
    required_working_minutes  INT          NOT NULL DEFAULT 480,
    half_day_min_minutes       INT          NOT NULL DEFAULT 240,
    full_day_min_minutes       INT          NOT NULL DEFAULT 420,
    late_grace_minutes         INT          NOT NULL DEFAULT 10,
    default_break_minutes      INT          NOT NULL DEFAULT 45,
    allow_multiple_checkin     BOOLEAN      NOT NULL DEFAULT FALSE,
    weekly_off_days            VARCHAR(100) NOT NULL DEFAULT 'SUNDAY',
    created_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_rule_config_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_attendance_rule_config_company UNIQUE (client_company_id)
) ENGINE=InnoDB;

INSERT INTO attendance_rule_config (client_company_id)
SELECT id FROM client_companies
WHERE id NOT IN (SELECT client_company_id FROM attendance_rule_config);
