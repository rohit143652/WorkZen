export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'HALF_DAY' | 'ON_LEAVE' | 'WORKING';

export const ATTENDANCE_STATUSES: { value: AttendanceStatus; label: string }[] = [
  { value: 'PRESENT', label: 'Present' },
  { value: 'ABSENT', label: 'Absent' },
  { value: 'HALF_DAY', label: 'Half Day' },
  { value: 'ON_LEAVE', label: 'On Leave' }
];

export type WorkMode = 'OFFICE' | 'WORK_FROM_HOME' | 'FIELD_WORK';

export const WORK_MODES: { value: WorkMode; label: string }[] = [
  { value: 'OFFICE', label: 'Office' },
  { value: 'WORK_FROM_HOME', label: 'Work From Home' },
  { value: 'FIELD_WORK', label: 'Field Work' }
];

export type AttendanceSource = 'SELF' | 'ADMIN' | 'SYSTEM' | 'BIOMETRIC' | 'QR' | 'API';

export type CorrectionRequestType =
  | 'MISSING_CHECK_IN' | 'MISSING_CHECK_OUT' | 'WRONG_CHECK_IN' | 'WRONG_CHECK_OUT' | 'OTHER';

export const CORRECTION_REQUEST_TYPES: { value: CorrectionRequestType; label: string }[] = [
  { value: 'MISSING_CHECK_IN', label: 'Missing check-in' },
  { value: 'MISSING_CHECK_OUT', label: 'Missing check-out' },
  { value: 'WRONG_CHECK_IN', label: 'Wrong check-in time' },
  { value: 'WRONG_CHECK_OUT', label: 'Wrong check-out time' },
  { value: 'OTHER', label: 'Other' }
];

export type CorrectionRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface MarkAttendanceRequest {
  employeeId: number;
  attendanceDate: string;
  status: AttendanceStatus;
  remarks?: string;
  /** Optional - supply both to compute hours/late/early-exit/status the same way a self check-out does. */
  checkInTime?: string;
  checkOutTime?: string;
  workMode?: WorkMode;
}

export interface BulkAttendanceEntry {
  employeeId: number;
  status: AttendanceStatus;
  remarks?: string;
  checkInTime?: string;
  checkOutTime?: string;
  workMode?: WorkMode;
}

export interface BulkMarkAttendanceRequest {
  attendanceDate: string;
  entries: BulkAttendanceEntry[];
  latitude?: number;
  longitude?: number;
}

export interface BulkMarkAttendanceResult {
  requested: number;
  marked: number;
  rejected: string[];
}

export interface UpdateAttendanceRequest {
  status: AttendanceStatus;
  remarks?: string;
  checkInTime?: string;
  checkOutTime?: string;
  modificationReason?: string;
}

export interface CheckInRequest {
  workMode?: WorkMode;
  latitude?: number;
  longitude?: number;
}

export interface CheckOutRequest {
  latitude?: number;
  longitude?: number;
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
  checkInTime?: string;
  checkOutTime?: string;
  grossWorkMinutes?: number;
  breakMinutes?: number;
  netWorkMinutes?: number;
  workMode?: WorkMode;
  attendanceSource?: AttendanceSource;
  late: boolean;
  lateMinutes?: number;
  earlyExit: boolean;
  earlyExitMinutes?: number;
  createdByRole?: string;
  modifiedByRole?: string;
  modificationReason?: string;
}

export interface CorrectionRequestCreateRequest {
  attendanceDate: string;
  requestType: CorrectionRequestType;
  requestedCheckIn?: string;
  requestedCheckOut?: string;
  reason: string;
}

export interface CorrectionReviewRequest {
  remarks?: string;
}

export interface CorrectionRequestResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  attendanceDate: string;
  requestType: CorrectionRequestType;
  requestedCheckIn?: string;
  requestedCheckOut?: string;
  reason: string;
  status: CorrectionRequestStatus;
  reviewedByUsername?: string;
  reviewedAt?: string;
  reviewRemarks?: string;
  createdAt: string;
}

export interface AttendanceRuleConfigResponse {
  id: number;
  officeStartTime: string;
  officeEndTime: string;
  requiredWorkingMinutes: number;
  halfDayMinMinutes: number;
  fullDayMinMinutes: number;
  lateGraceMinutes: number;
  defaultBreakMinutes: number;
  allowMultipleCheckin: boolean;
  weeklyOffDays: string;
}

export interface UpdateAttendanceRuleConfigRequest {
  officeStartTime: string;
  officeEndTime: string;
  requiredWorkingMinutes: number;
  halfDayMinMinutes: number;
  fullDayMinMinutes: number;
  lateGraceMinutes: number;
  defaultBreakMinutes: number;
  allowMultipleCheckin: boolean;
  weeklyOffDays: string;
}

export interface TodayAttendanceOverviewEntry {
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  checkInTime?: string;
  checkOutTime?: string;
  workMode?: WorkMode;
  late: boolean;
}

export interface TodayAttendanceOverviewResponse {
  date: string;
  totalActiveEmployees: number;
  checkedInCount: number;
  notCheckedInCount: number;
  onLeaveCount: number;
  checkedIn: TodayAttendanceOverviewEntry[];
  notCheckedIn: TodayAttendanceOverviewEntry[];
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
