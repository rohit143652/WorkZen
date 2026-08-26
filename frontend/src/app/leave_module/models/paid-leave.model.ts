/**
 * Effective-dated leave policy (architecture refactor Phase 9) - a
 * tenant can have any number of these over time, each covering a date
 * range. id/effectiveFrom/effectiveTo/status are only present on rows
 * read from the API (history, current, for-month) - omit them when
 * submitting a new policy.
 */
export interface PaidLeaveConfig {
  id?: number;
  effectiveFrom?: string;
  effectiveTo?: string | null;
  status?: 'ACTIVE' | 'CANCELLED';
  monthlyPaidLeave: number;
  /** Master switch - client decides whether Paid Leave is active or fully off. False = no new monthly entitlement accrues while this policy is in effect. */
  enabled: boolean;
  allowCarryForward: boolean;
  maximumCarryForward: number | null;
  resetAnnually: boolean;
}

/** What's submitted to schedule a new policy - effectiveFrom is required here even though it's optional on the read-side PaidLeaveConfig shape above. */
export interface PaidLeaveConfigCreateRequest {
  effectiveFrom: string;
  monthlyPaidLeave: number;
  enabled: boolean;
  allowCarryForward: boolean;
  maximumCarryForward: number | null;
  resetAnnually: boolean;
}

export interface EmployeeLeaveSummary {
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  availableLeave: number;
}

export interface EmployeePaidLeaveBalance {
  year: number;
  month: number;
  monthlyAllocation: number;
  carryForward: number;
  extraLeave: number;
  usedLeave: number;
  availableLeave: number;
  manualOverride: boolean;
}

export type ExtraLeaveReason = 'MEDICAL' | 'SPECIAL' | 'EMERGENCY' | 'OTHER';

export const EXTRA_LEAVE_REASONS: { value: ExtraLeaveReason; label: string }[] = [
  { value: 'MEDICAL', label: 'Medical' },
  { value: 'SPECIAL', label: 'Special' },
  { value: 'EMERGENCY', label: 'Emergency' },
  { value: 'OTHER', label: 'Other' }
];

export interface ExtraPaidLeaveRequest {
  leaveDays: number;
  reason: ExtraLeaveReason;
  startDate: string;
  endDate?: string | null;
  remark?: string;
}

export interface ExtraPaidLeaveResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  leaveDays: number;
  reason: ExtraLeaveReason;
  startDate: string;
  endDate: string | null;
  remark: string | null;
  status: 'ACTIVE' | 'CANCELLED';
  createdAt: string;
  grantedBy: string;
}
