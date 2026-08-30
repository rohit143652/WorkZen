/**
 * There is no "category" column on Permission in the database - this derives one purely from
 * the permission NAME's prefix, for display grouping only. Purely cosmetic: doesn't change
 * which permissions exist or what they do, just how they're grouped when shown to an admin
 * picking permissions for a role. Anything that doesn't match a known prefix falls into
 * "Other" rather than being silently dropped, so a newly-added permission always still shows
 * up somewhere even before this mapping is updated for it.
 */
const CATEGORY_RULES: { category: string; prefixes: string[] }[] = [
  { category: 'Dashboard', prefixes: ['DASHBOARD_', 'CLIENT_DASHBOARD_'] },
  { category: 'Users & Login', prefixes: ['USER_', 'PASSWORD_'] },
  { category: 'Roles & Permissions', prefixes: ['ROLE_', 'PERMISSION_'] },
  { category: 'Client Companies', prefixes: ['CLIENT_PROFILE_'] },
  { category: 'Employees', prefixes: ['EMPLOYEE_'] },
  { category: 'Sites', prefixes: ['SITE_'] },
  { category: 'Departments & Designations', prefixes: ['DEPARTMENT_', 'DESIGNATION_'] },
  { category: 'Attendance', prefixes: ['ATTENDANCE_', 'MONTHLY_PAYMENT_REPORT_'] },
  { category: 'Holiday Calendar', prefixes: ['HOLIDAY_'] },
  { category: 'Paid Leave', prefixes: ['PAID_LEAVE_'] },
  { category: 'Salary Structure', prefixes: ['SALARY_STRUCTURE_', 'SALARY_ASSIGN', 'SALARY_COMPONENT_'] },
  { category: 'Payroll', prefixes: ['PAYROLL_'] },
  { category: 'Payslip', prefixes: ['PAYSLIP_'] },
  { category: 'Advances', prefixes: ['ADVANCE_'] },
  { category: 'Exit Management', prefixes: ['EMPLOYEE_EXIT_'] }
];

export function getPermissionCategory(permissionName: string): string {
  for (const rule of CATEGORY_RULES) {
    if (rule.prefixes.some(prefix => permissionName.startsWith(prefix))) {
      return rule.category;
    }
  }
  return 'Other';
}

export interface PermissionGroup<T> {
  category: string;
  permissions: T[];
}

/** Groups a flat permission list by category, sorted alphabetically by category name so the grouping is stable and predictable rather than depending on original list order. */
export function groupPermissionsByCategory<T extends { name: string }>(permissions: T[]): PermissionGroup<T>[] {
  const map = new Map<string, T[]>();
  for (const p of permissions) {
    const category = getPermissionCategory(p.name);
    if (!map.has(category)) map.set(category, []);
    map.get(category)!.push(p);
  }
  return Array.from(map.entries())
    .map(([category, perms]) => ({ category, permissions: perms }))
    .sort((a, b) => a.category.localeCompare(b.category));
}
