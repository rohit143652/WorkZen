import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdvanceDashboardSummary, AdvanceGrantRequest, AdvancePartialSettlementRequest, AdvanceRecoveryTransactionResponse, EmployeeAdvanceResponse } from '../models/advance.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class EmployeeAdvanceService {
  private readonly http = inject(HttpClient);

  private url(employeeId: number): string {
    return `${environment.apiUrl}/employees/${employeeId}/advances`;
  }

  list(employeeId: number): Observable<EmployeeAdvanceResponse[]> {
    return this.http.get<ApiEnvelope<EmployeeAdvanceResponse[]>>(this.url(employeeId)).pipe(map(e => e.data));
  }

  /** Every advance across every employee for the tenant - for the Advance Dashboard. */
  listAll(): Observable<EmployeeAdvanceResponse[]> {
    return this.http.get<ApiEnvelope<EmployeeAdvanceResponse[]>>(`${environment.apiUrl}/advances`).pipe(map(e => e.data));
  }

  getDashboardSummary(): Observable<AdvanceDashboardSummary> {
    return this.http.get<ApiEnvelope<AdvanceDashboardSummary>>(`${environment.apiUrl}/advances/summary`).pipe(map(e => e.data));
  }

  grant(employeeId: number, request: AdvanceGrantRequest): Observable<EmployeeAdvanceResponse> {
    return this.http.post<ApiEnvelope<EmployeeAdvanceResponse>>(this.url(employeeId), request).pipe(map(e => e.data));
  }

  updateRecoveryAmount(employeeId: number, advanceId: number, monthlyRecoveryAmount: number): Observable<EmployeeAdvanceResponse> {
    return this.http
      .put<ApiEnvelope<EmployeeAdvanceResponse>>(`${this.url(employeeId)}/${advanceId}/recovery-amount`, { monthlyRecoveryAmount })
      .pipe(map(e => e.data));
  }

  /** Pauses or resumes payroll-based recovery for this advance - manual settlement is unaffected either way. */
  updateRecoverViaPayroll(employeeId: number, advanceId: number, recoverViaPayroll: boolean): Observable<EmployeeAdvanceResponse> {
    return this.http
      .put<ApiEnvelope<EmployeeAdvanceResponse>>(`${this.url(employeeId)}/${advanceId}/recover-via-payroll`, { recoverViaPayroll })
      .pipe(map(e => e.data));
  }

  /** Full settlement - writes off the entire remaining outstanding, stopping all future payroll recovery. */
  settle(employeeId: number, advanceId: number): Observable<EmployeeAdvanceResponse> {
    return this.http
      .put<ApiEnvelope<EmployeeAdvanceResponse>>(`${this.url(employeeId)}/${advanceId}/settle`, {})
      .pipe(map(e => e.data));
  }

  /** Partial settlement - the employee paid some of the outstanding amount directly, outside payroll. Future payroll recovers from the reduced outstanding only. */
  settlePartial(employeeId: number, advanceId: number, request: AdvancePartialSettlementRequest): Observable<EmployeeAdvanceResponse> {
    return this.http
      .put<ApiEnvelope<EmployeeAdvanceResponse>>(`${this.url(employeeId)}/${advanceId}/settle-partial`, request)
      .pipe(map(e => e.data));
  }

  /** Every recovery event for one advance - answers "which payroll (or manual payment) recovered this amount?" */
  getRecoveryHistory(employeeId: number, advanceId: number): Observable<AdvanceRecoveryTransactionResponse[]> {
    return this.http
      .get<ApiEnvelope<AdvanceRecoveryTransactionResponse[]>>(`${this.url(employeeId)}/${advanceId}/recovery-history`)
      .pipe(map(e => e.data));
  }
}
