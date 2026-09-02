-- ============================================================
-- V94: Events (for the unified Calendar - merges with existing company-wide Holidays)
--
-- Holiday (see V85) is already company-wide and reused as-is - no changes to it. Event is new:
-- can be timed (start/end) or all-day, and its visibility is either ALL_USERS (every employee
-- in the tenant sees it) or SELECTED_USERS (only the specific employees in event_participants
-- see it) - this is a DIFFERENT visibility model from Holiday's always-company-wide rule, kept
-- deliberately separate per business rule #30 even though both render in the same Calendar UI.
-- ============================================================

CREATE TABLE events (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT       NOT NULL,
    title               VARCHAR(200) NOT NULL,
    description         VARCHAR(1000),
    location            VARCHAR(200),
    start_at            DATETIME     NOT NULL,
    end_at              DATETIME     NOT NULL,
    all_day             BOOLEAN      NOT NULL DEFAULT FALSE,
    visibility          VARCHAR(20)  NOT NULL DEFAULT 'SELECTED_USERS',

    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT chk_event_times CHECK (end_at >= start_at),
    CONSTRAINT chk_event_visibility CHECK (visibility IN ('ALL_USERS', 'SELECTED_USERS'))
);

CREATE INDEX idx_event_company_range ON events (client_company_id, start_at, end_at);

-- Only populated when visibility = SELECTED_USERS - an ALL_USERS event has no rows here at all
-- (its visibility is resolved by tenant membership alone, not by this join table).
CREATE TABLE event_participants (
    event_id     BIGINT NOT NULL,
    employee_id  BIGINT NOT NULL,
    PRIMARY KEY (event_id, employee_id),
    CONSTRAINT fk_event_participant_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_participant_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE
);

INSERT INTO permissions (name, description) VALUES
    ('EVENT_READ',   'View calendar events'),
    ('EVENT_CREATE', 'Create calendar events'),
    ('EVENT_UPDATE', 'Edit calendar events'),
    ('EVENT_DELETE', 'Delete calendar events');

-- Matches the existing pattern (V25/V91) - CLIENT_ADMIN and every standard company role gets
-- full event access; event creation is a normal day-to-day action, not an admin-only one like
-- Holiday remains (HOLIDAY_CREATE stays exactly as restricted as it already was - untouched
-- here).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE p.name IN ('EVENT_READ', 'EVENT_CREATE', 'EVENT_UPDATE', 'EVENT_DELETE')
  AND (
       r.name IN ('CLIENT_ADMIN', 'CLIENT_USER')
    OR r.name IN ('ADMIN', 'HR_ADMIN', 'SITE_ADMIN', 'SITE_SUPERVISOR', 'ACCOUNTANT')
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
