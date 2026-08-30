export interface EmployeeLoginAccess {
  username: string;
  password: string;
  confirmPassword: string;
  roleId: number | null;
}

export interface EmployeeRequest {
  employeeCode: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  email: string;
  mobileNumber?: string;
  alternateMobileNumber?: string;
  dateOfBirth?: string;
  gender?: string;
  joiningDate: string;
  department: string;
  designation: string;
  employmentType?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  pincode?: string;
  aadharNumber: string;
  panNumber: string;
  /** PF/ESI/PT deduction applicability for this specific employee - independent of Salary Structure type. Both this AND the tenant's Payroll Settings must be enabled for a deduction to apply. Omit to keep the default (true). */
  pfApplicable?: boolean;
  esiApplicable?: boolean;
  ptApplicable?: boolean;
  enableLogin: boolean;
  loginAccess?: EmployeeLoginAccess;
  /** Optional: assign a Salary Structure at creation/edit time. Only takes effect if the caller has EMPLOYEE_SALARY_UPDATE - see EmployeeService. */
  salaryStructureId?: number | null;
  /** Required when salaryStructureId is set on an update (a real reassignment); defaults to joiningDate on create if left blank. */
  salaryEffectiveFrom?: string | null;
}

export interface EmployeeUpdateRequest extends Omit<EmployeeRequest, 'employeeCode' | 'enableLogin' | 'loginAccess'> {}

export interface EmployeeResponse {
  id: number;
  employeeCode: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  email: string;
  mobileNumber?: string;
  alternateMobileNumber?: string;
  dateOfBirth?: string;
  gender?: string;
  joiningDate: string;
  department: string;
  designation: string;
  employmentType?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  pincode?: string;
  aadharNumber?: string;
  panNumber?: string;
  pfApplicable: boolean;
  esiApplicable: boolean;
  ptApplicable: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
  loginEnabled: boolean;
  userId?: number;
  username?: string;
  userActive?: boolean;
  userLocked?: boolean;
  roles?: string[];
  lastLoginAt?: string;
  /** True only if the current caller has EMPLOYEE_SALARY_READ - all salary fields below are undefined otherwise. */
  salaryVisible: boolean;
  currentSalaryStructureId?: number;
  currentSalaryStructureCode?: string;
  currentSalaryStructureName?: string;
  currentSalaryType?: string;
  currentSalaryEffectiveFrom?: string;
  currentGrossEarnings?: number;
}

export interface EnableLoginRequest {
  username?: string;
  password?: string;
  roleId?: number | null;
}

export interface AssignRoleRequest {
  roleId: number;
}

export interface EmployeeBulkImportRowError {
  rowNumber: number;
  reason: string;
}

export interface EmployeeBulkImportResult {
  totalRows: number;
  successCount: number;
  failureCount: number;
  errors: EmployeeBulkImportRowError[];
}
