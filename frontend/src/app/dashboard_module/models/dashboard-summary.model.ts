export interface DashboardSummary {
  global: boolean;
  totalEmployees: number;
  activeEmployees: number;
  employeesWithLogin: number;
  employeesWithoutLogin: number;
  activeUsers: number;
  lockedUsers: number;
  departments: number;
  assignedEmployees: number;
  unassignedEmployees: number;
  totalSites: number;
  totalClientCompanies: number;
  activeClientCompanies: number;
}
