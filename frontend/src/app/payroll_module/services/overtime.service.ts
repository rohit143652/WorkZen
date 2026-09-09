import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OvertimeRecord, OvertimeRecordRequest } from '../models/payroll.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

/**
 * The Overtime Register - day-wise, auditable log of who did overtime, when, and how many
 * hours (see backend EmployeeOvertimeRecord/V107 migration for why this exists instead of a
 * single monthly number). A month's payroll overtime total is always summed from these records
 * server-side, never typed in directly on the Payroll Run screen any more.
 */
@Injectable({ providedIn: 'root' })
export class OvertimeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/overtime-records`;

  mark(request: OvertimeRecordRequest): Observable<OvertimeRecord> {
    return this.http.post<ApiEnvelope<OvertimeRecord>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  delete(id: number, employeeId: number): Observable<void> {
    const params = new HttpParams().set('employeeId', employeeId);
    return this.http.delete<ApiEnvelope<void>>(`${this.baseUrl}/${id}`, { params }).pipe(map(() => void 0));
  }

  forEmployee(employeeId: number, from: string, to: string): Observable<OvertimeRecord[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<ApiEnvelope<OvertimeRecord[]>>(`${this.baseUrl}/employee/${employeeId}`, { params }).pipe(map(e => e.data));
  }
}
