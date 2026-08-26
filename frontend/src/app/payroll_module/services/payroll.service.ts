import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PayrollSettings, PayrollSettingsCreateRequest } from '../models/payroll.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

/** Effective-dated payroll configuration (architecture refactor Phase 8) - see PayrollSettings.model.ts for the shape. */
@Injectable({ providedIn: 'root' })
export class PayrollService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/payroll`;

  /** The configuration in effect today. */
  getSettings(): Observable<PayrollSettings> {
    return this.http.get<ApiEnvelope<PayrollSettings>>(`${this.baseUrl}/settings`).pipe(map(e => e.data));
  }

  /** Full configuration timeline, newest first. */
  getHistory(): Observable<PayrollSettings[]> {
    return this.http.get<ApiEnvelope<PayrollSettings[]>>(`${this.baseUrl}/settings/history`).pipe(map(e => e.data));
  }

  /** What would apply to a specific payroll month, past or future. */
  getForMonth(year: number, month: number): Observable<PayrollSettings> {
    const params = new HttpParams().set('year', year).set('month', month);
    return this.http.get<ApiEnvelope<PayrollSettings>>(`${this.baseUrl}/settings/for-month`, { params }).pipe(map(e => e.data));
  }

  /** Schedules a new configuration (today or a future effectiveFrom) - never edits a past/current rate in place. */
  createConfig(request: PayrollSettingsCreateRequest): Observable<PayrollSettings> {
    return this.http.post<ApiEnvelope<PayrollSettings>>(`${this.baseUrl}/settings`, request).pipe(map(e => e.data));
  }

  /** Edits a configuration that hasn't taken effect yet. */
  updateConfig(id: number, request: PayrollSettingsCreateRequest): Observable<PayrollSettings> {
    return this.http.put<ApiEnvelope<PayrollSettings>>(`${this.baseUrl}/settings/${id}`, request).pipe(map(e => e.data));
  }

  /** Cancels a not-yet-effective configuration. */
  cancelConfig(id: number): Observable<void> {
    return this.http.delete<ApiEnvelope<void>>(`${this.baseUrl}/settings/${id}`).pipe(map(() => void 0));
  }
}
