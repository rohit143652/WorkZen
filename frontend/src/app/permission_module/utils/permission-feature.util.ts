/**
 * Company Feature Configuration (Super Admin, "Manage Features") is the upper-level control - a
 * permission like ATTENDANCE_SELF_MARK is meaningless if the company itself has Employee Self
 * Attendance switched off, so such a permission isn't even OFFERED as a checkbox while its feature
 * is off (hidden outright, not shown-but-disabled: there is nothing useful to do with it either way).
 * Shared by every screen that hands out permissions (role editor, an employee's additional
 * permissions) so they can never disagree about what is available. Multiple codes are AND'd: every
 * one must be on for the permission to appear.
 */
export const PERMISSION_FEATURE_DEPENDENCIES: Record<string, string[]> = {
  ATTENDANCE_SELF_MARK: ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE'],
  ATTENDANCE_CREATE: ['ATTENDANCE_MANAGEMENT'],
  ATTENDANCE_CORRECTION_REQUEST: ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE'],
  ATTENDANCE_CORRECTION_REVIEW: ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE'],
  ATTENDANCE_RULES_MANAGE: ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE']
};

/** False only for a permission whose dependent company feature(s) are currently OFF; a permission with no such dependency is always available. */
export function isPermissionFeatureAvailable(name: string, isFeatureEnabled: (code: string) => boolean): boolean {
  const codes = PERMISSION_FEATURE_DEPENDENCIES[name];
  if (!codes) return true;
  return codes.every(code => isFeatureEnabled(code));
}
