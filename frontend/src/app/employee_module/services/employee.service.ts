import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import { AssignRoleRequest, EmployeeRequest, EmployeeResponse, EmployeeUpdateRequest, EnableLoginRequest } from '../models/employee.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

export interface EmployeeSearchParams {
  search?: string;
  status?: string;
  department?: string;
  loginEnabled?: boolean;
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
}
