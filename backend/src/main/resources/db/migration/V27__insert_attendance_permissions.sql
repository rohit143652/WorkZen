-- ============================================================
-- V27: Attendance permissions
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('ATTENDANCE_CREATE', 'Mark attendance for today or a previous date, for an actively-assigned employee'),
    ('ATTENDANCE_READ',   'View attendance records and history'),
    ('ATTENDANCE_UPDATE', 'Change an already-marked attendance record (normally Client Admin only)');
