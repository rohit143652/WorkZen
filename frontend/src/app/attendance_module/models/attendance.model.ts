export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'HALF_DAY' | 'ON_LEAVE';

export const ATTENDANCE_STATUSES: { value: AttendanceStatus; label: string }[] = [
  { value: 'PRESENT', label: 'Present' },
  { value: 'ABSENT', label: 'Absent' },
  { value: 'HALF_DAY', label: 'Half Day' },
  { value: 'ON_LEAVE', label: 'On Leave' }
];

export interface MarkAttendanceRequest {
  employeeId: number;
  attendanceDate: string;
  status: AttendanceStatus;
  remarks?: string;
}

export interface BulkAttendanceEntry {
  employeeId: number;
  status: AttendanceStatus;
  remarks?: string;
}

export interface BulkMarkAttendanceRequest {
  attendanceDate: string;
  entries: BulkAttendanceEntry[];
}

export interface BulkMarkAttendanceResult {
  requested: number;
  marked: number;
  rejected: string[];
}

export interface UpdateAttendanceRequest {
  status: AttendanceStatus;
  remarks?: string;
}

export interface AttendanceResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  siteId: number;
  siteName: string;
  attendanceDate: string;
  status: AttendanceStatus;
  remarks?: string;
  markedByUsername?: string;
  updatedByUsername?: string;
  createdAt: string;
  updatedAt: string;
  editable: boolean;
}

export interface EmployeeAttendanceOption {
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  siteId: number;
  siteName: string;
  existingRecord: AttendanceResponse | null;
}

export interface MonthlyAttendanceReportRow {
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  department: string;
  designation: string;
  currentSite: string;
  presentDays: number;
  halfDays: number;
  onLeaveDays: number;
  absentDays: number;
  paidLeaveDays: number;
  unpaidLeaveDays: number;
  payableDays: number;
  leaveBalanceOpening: number;
  leaveBalanceClosing: number;
  manualLeaveOverride: boolean;
}

export interface MonthlyAttendanceReportResponse {
  year: number;
  month: number;
  monthLabel: string;
  daysInMonth: number;
  rows: MonthlyAttendanceReportRow[];
}
