import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  EmployeeLeaveSummary, EmployeePaidLeaveBalance, ExtraPaidLeaveRequest, ExtraPaidLeaveResponse, PaidLeaveConfig, PaidLeaveConfigCreateRequest
} from '../models/paid-leave.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class PaidLeaveService {
  private readonly http = inject(HttpClient);
  private readonly configUrl = `${environment.apiUrl}/paid-leave/config`;

  private employeeUrl(employeeId: number): string {
    return `${environment.apiUrl}/employees/${employeeId}/paid-leave`;
  }

  /** The policy in effect today. */
  getConfig(): Observable<PaidLeaveConfig> {
    return this.http.get<ApiEnvelope<PaidLeaveConfig>>(this.configUrl).pipe(map(e => e.data));
  }

  /** Full policy timeline, newest first. */
  getConfigHistory(): Observable<PaidLeaveConfig[]> {
    return this.http.get<ApiEnvelope<PaidLeaveConfig[]>>(`${this.configUrl}/history`).pipe(map(e => e.data));
  }

  /** What would apply to a specific leave month, past or future. */
  getConfigForMonth(year: number, month: number): Observable<PaidLeaveConfig> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<ApiEnvelope<PaidLeaveConfig>>(`${this.configUrl}/for-month`, { params }).pipe(map(e => e.data));
  }

  /** Schedules a new policy (today or a future effectiveFrom) - never edits a past/current policy in place. */
  createConfig(request: PaidLeaveConfigCreateRequest): Observable<PaidLeaveConfig> {
    return this.http.post<ApiEnvelope<PaidLeaveConfig>>(this.configUrl, request).pipe(map(e => e.data));
  }

  /** Edits a policy that hasn't taken effect yet. */
  updateConfig(id: number, request: PaidLeaveConfigCreateRequest): Observable<PaidLeaveConfig> {
    return this.http.put<ApiEnvelope<PaidLeaveConfig>>(`${this.configUrl}/${id}`, request).pipe(map(e => e.data));
  }

  /** Cancels a not-yet-effective policy. */
  cancelConfig(id: number): Observable<void> {
    return this.http.delete<ApiEnvelope<void>>(`${this.configUrl}/${id}`).pipe(map(() => void 0));
  }

  /** Every active employee's current total available paid leave - for the Paid Leave Settings overview. */
  listEmployeeBalances(): Observable<EmployeeLeaveSummary[]> {
    return this.http.get<ApiEnvelope<EmployeeLeaveSummary[]>>(`${this.configUrl}/employee-balances`).pipe(map(e => e.data));
  }

  getCurrentBalance(employeeId: number): Observable<EmployeePaidLeaveBalance> {
    return this.http.get<ApiEnvelope<EmployeePaidLeaveBalance>>(this.employeeUrl(employeeId)).pipe(map(e => e.data));
  }

  getBalanceHistory(employeeId: number): Observable<EmployeePaidLeaveBalance[]> {
    return this.http.get<ApiEnvelope<EmployeePaidLeaveBalance[]>>(`${this.employeeUrl(employeeId)}/history`).pipe(map(e => e.data));
  }

  listExtraLeave(employeeId: number): Observable<ExtraPaidLeaveResponse[]> {
    return this.http.get<ApiEnvelope<ExtraPaidLeaveResponse[]>>(`${this.employeeUrl(employeeId)}/extra`).pipe(map(e => e.data));
  }

  grantExtraLeave(employeeId: number, request: ExtraPaidLeaveRequest): Observable<ExtraPaidLeaveResponse> {
    return this.http.post<ApiEnvelope<ExtraPaidLeaveResponse>>(`${this.employeeUrl(employeeId)}/extra`, request).pipe(map(e => e.data));
  }

  updateExtraLeave(employeeId: number, id: number, request: ExtraPaidLeaveRequest): Observable<ExtraPaidLeaveResponse> {
    return this.http.put<ApiEnvelope<ExtraPaidLeaveResponse>>(`${this.employeeUrl(employeeId)}/extra/${id}`, request).pipe(map(e => e.data));
  }

  cancelExtraLeave(employeeId: number, id: number): Observable<void> {
    return this.http.put<ApiEnvelope<void>>(`${this.employeeUrl(employeeId)}/extra/${id}/cancel`, {}).pipe(map(() => void 0));
  }
}
