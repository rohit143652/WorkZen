import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import { EmployeeResponse } from '../../employee_module/models/employee.model';
import { EmployeeService, EmployeeSearchParams } from '../../employee_module/services/employee.service';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

/**
 * User Management is primarily a view of employees that DO have a login
 * account (see spec section 9/52) plus login-lifecycle actions. It
 * deliberately reuses EmployeeService/EmployeeController for
 * enable/disable/role-change so there is exactly one code path that
 * mutates login state, and calls the existing generic UserController only
 * for account-unlock and password-reset, which are user-account-scoped
 * concerns pre-dating this module.
 */
@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly http = inject(HttpClient);
  private readonly employeeService = inject(EmployeeService);

  list(params: Omit<EmployeeSearchParams, 'loginEnabled'>): Observable<PageResult<EmployeeResponse>> {
    return this.employeeService.search({ ...params, loginEnabled: true });
  }

  unlock(userId: number): Observable<void> {
    return this.http
      .put<ApiEnvelope<void>>(`${environment.apiUrl}/users/${userId}/unlock`, {})
      .pipe(map(() => void 0));
  }

  resetPassword(userId: number): Observable<string> {
    return this.http
      .post<ApiEnvelope<{ temporaryPassword: string }>>(`${environment.apiUrl}/users/${userId}/reset-password`, {})
      .pipe(map(e => e.data.temporaryPassword));
  }

  /** Admin-chosen password, as distinct from resetPassword's randomly-generated one. */
  setPassword(userId: number, newPassword: string, confirmPassword: string): Observable<void> {
    return this.http
      .put<ApiEnvelope<void>>(`${environment.apiUrl}/users/${userId}/set-password`, { newPassword, confirmPassword })
      .pipe(map(() => void 0));
  }

  /** Auto-generates the next available username from an employee's name - called the moment "Login Enabled" is switched on in the Employee form. See UserService.generateUsername() for the exact fallback order. */
  generateUsername(firstName: string, lastName: string): Observable<string> {
    const params = new HttpParams().set('firstName', firstName).set('lastName', lastName);
    return this.http
      .get<ApiEnvelope<string>>(`${environment.apiUrl}/users/generate-username`, { params })
      .pipe(map(e => e.data));
  }
}
