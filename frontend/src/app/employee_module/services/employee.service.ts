import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import { AssignRoleRequest, EmployeeBulkImportResult, EmployeeRequest, EmployeeResponse, EmployeeUpdateRequest, EnableLoginRequest, ProfileCompletion, SelfProfileUpdateRequest } from '../models/employee.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

export interface EmployeeSearchParams {
  search?: string;
  status?: string;
  department?: string;
  loginEnabled?: boolean;
  /** 'ONBOARDING_PENDING' | 'MANDATORY_PROFILE_INCOMPLETE' | 'PROFILE_COMPLETE' - see backend EmployeeService.onboardingStatusesFor() for exactly what each maps to. */
  onboardingFilter?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/employees`;

  search(params: EmployeeSearchParams): Observable<PageResult<EmployeeResponse>> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return this.http
      .get<ApiEnvelope<PageResult<EmployeeResponse>>>(this.baseUrl, { params: httpParams })
      .pipe(map(envelope => envelope.data));
  }

  getById(id: number): Observable<EmployeeResponse> {
    return this.http.get<ApiEnvelope<EmployeeResponse>>(`${this.baseUrl}/${id}`).pipe(map(e => e.data));
  }

  /** Preview of the code the Add form should show (disabled) - the backend generates the same code again on submit if left blank. */
  nextCode(): Observable<string> {
    return this.http.get<ApiEnvelope<{ code: string }>>(`${this.baseUrl}/next-code`).pipe(map(e => e.data.code));
  }

  create(request: EmployeeRequest): Observable<EmployeeResponse> {
    return this.http.post<ApiEnvelope<EmployeeResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  update(id: number, request: EmployeeUpdateRequest): Observable<EmployeeResponse> {
    return this.http.put<ApiEnvelope<EmployeeResponse>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  activate(id: number): Observable<EmployeeResponse> {
    return this.http.put<ApiEnvelope<EmployeeResponse>>(`${this.baseUrl}/${id}/activate`, {}).pipe(map(e => e.data));
  }

  /** For an ex-employee coming back - reactivates this SAME record (keeping all their history) but with a freshly generated employeeCode, distinct from a plain activate() which keeps the old code. */
  rejoin(id: number): Observable<EmployeeResponse> {
    return this.http.put<ApiEnvelope<EmployeeResponse>>(`${this.baseUrl}/${id}/rejoin`, {}).pipe(map(e => e.data));
  }

  deactivate(id: number): Observable<EmployeeResponse> {
    return this.http.put<ApiEnvelope<EmployeeResponse>>(`${this.baseUrl}/${id}/deactivate`, {}).pipe(map(e => e.data));
  }

  enableLogin(id: number, request: EnableLoginRequest): Observable<EmployeeResponse> {
    return this.http.post<ApiEnvelope<EmployeeResponse>>(`${this.baseUrl}/${id}/enable-login`, request).pipe(map(e => e.data));
  }

  disableLogin(id: number): Observable<EmployeeResponse> {
    return this.http.post<ApiEnvelope<EmployeeResponse>>(`${this.baseUrl}/${id}/disable-login`, {}).pipe(map(e => e.data));
  }

  assignRole(id: number, request: AssignRoleRequest): Observable<EmployeeResponse> {
    return this.http.put<ApiEnvelope<EmployeeResponse>>(`${this.baseUrl}/${id}/role`, request).pipe(map(e => e.data));
  }

  resetPassword(id: number): Observable<string> {
    return this.http
      .post<ApiEnvelope<{ temporaryPassword: string }>>(`${this.baseUrl}/${id}/reset-password`, {})
      .pipe(map(e => e.data.temporaryPassword));
  }

  downloadImportTemplate(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/bulk-import/template`, { responseType: 'blob' });
  }

  bulkImport(file: File): Observable<EmployeeBulkImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiEnvelope<EmployeeBulkImportResult>>(`${this.baseUrl}/bulk-import`, formData)
      .pipe(map(e => e.data));
  }

  /** Admin/HR action - invalidates any existing invitation for this employee and sends a fresh one (see backend EmployeeOnboardingService.resendInvitation()). */
  resendInvitation(employeeId: number): Observable<void> {
    return this.http.post<ApiEnvelope<void>>(`${environment.apiUrl}/onboarding/resend-invitation/${employeeId}`, {}).pipe(map(() => void 0));
  }

  /** The logged-in user's OWN full employee record - used to pre-fill the "My Profile" edit form before Save, so unedited fields aren't accidentally blanked out. */
  getMyProfile(): Observable<EmployeeResponse> {
    return this.http.get<ApiEnvelope<EmployeeResponse>>(`${this.baseUrl}/me`).pipe(map(e => e.data));
  }

  /** The logged-in user's OWN profile completion - resolved server-side from their own account, never from a client-supplied employee id. */
  getMyProfileCompletion(): Observable<ProfileCompletion> {
    return this.http.get<ApiEnvelope<ProfileCompletion>>(`${this.baseUrl}/me/profile-completion`).pipe(map(e => e.data));
  }

  /** Admin/HR view of any employee's profile completion. */
  getProfileCompletion(id: number): Observable<ProfileCompletion> {
    return this.http.get<ApiEnvelope<ProfileCompletion>>(`${this.baseUrl}/${id}/profile-completion`).pipe(map(e => e.data));
  }

  /** Self-service update - only EMPLOYEE_EDITABLE fields (see backend SelfProfileUpdateRequest javadoc for what's deliberately excluded). */
  updateMyProfile(request: SelfProfileUpdateRequest): Observable<ProfileCompletion> {
    return this.http.put<ApiEnvelope<ProfileCompletion>>(`${this.baseUrl}/me/profile`, request).pipe(map(e => e.data));
  }
}
