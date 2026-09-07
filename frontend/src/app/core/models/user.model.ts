export interface AuthenticatedUser {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
  permissions: string[];
  mustChangePassword?: boolean;
  /** Null for logins with no linked Employee (e.g. a SUPER_ADMIN account not tied to an employee record). */
  employeeCode?: string | null;
}
