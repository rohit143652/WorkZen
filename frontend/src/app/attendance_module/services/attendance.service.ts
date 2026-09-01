import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import {
  AttendanceResponse, BulkMarkAttendanceRequest, BulkMarkAttendanceResult,
  EmployeeAttendanceOption, MarkAttendanceRequest, MonthlyAttendanceReportResponse, UpdateAttendanceRequest
} from '../models/attendance.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/attendance`;

  markable(date: string, siteId?: number | null): Observable<EmployeeAttendanceOption[]> {
    let params = new HttpParams().set('date', date);
    if (siteId) params = params.set('siteId', siteId);
    return this.http.get<ApiEnvelope<EmployeeAttendanceOption[]>>(`${this.baseUrl}/markable`, { params }).pipe(map(e => e.data));
  }

  mark(request: MarkAttendanceRequest): Observable<AttendanceResponse> {
    return this.http.post<ApiEnvelope<AttendanceResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  bulkMark(request: BulkMarkAttendanceRequest): Observable<BulkMarkAttendanceResult> {
    return this.http.post<ApiEnvelope<BulkMarkAttendanceResult>>(`${this.baseUrl}/bulk`, request).pipe(map(e => e.data));
  }

  /** For "Mark My Attendance" (self-service) - null (as data) means today isn't marked yet. */
  myTodayStatus(): Observable<AttendanceResponse | null> {
    return this.http.get<ApiEnvelope<AttendanceResponse | null>>(`${this.baseUrl}/mine/today`).pipe(map(e => e.data));
  }

  /** One-click self-service mark - always today, always PRESENT (see backend for why). */
  markMine(latitude?: number, longitude?: number): Observable<AttendanceResponse> {
    return this.http.post<ApiEnvelope<AttendanceResponse>>(`${this.baseUrl}/mine`, { latitude, longitude }).pipe(map(e => e.data));
  }

  update(id: number, request: UpdateAttendanceRequest): Observable<AttendanceResponse> {
    return this.http.put<ApiEnvelope<AttendanceResponse>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  forEmployee(employeeId: number, from: string, to: string): Observable<AttendanceResponse[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http
      .get<ApiEnvelope<AttendanceResponse[]>>(`${this.baseUrl}/employee/${employeeId}`, { params })
      .pipe(map(e => e.data));
  }

  list(from: string, to: string, siteId: number | null, page: number, size: number): Observable<PageResult<AttendanceResponse>> {
    let params = new HttpParams().set('from', from).set('to', to).set('page', page).set('size', size);
    if (siteId) params = params.set('siteId', siteId);
    return this.http.get<ApiEnvelope<PageResult<AttendanceResponse>>>(this.baseUrl, { params }).pipe(map(e => e.data));
  }

  /** Bulk (all employees) monthly attendance + calculated-payment preview - JSON for the UI table. Paid leave is sourced from the Paid Leave module - see leave.service.ts. siteIds: empty/undefined = all sites. */
  monthlyReportPreview(year: number, month: number, siteIds?: number[]): Observable<MonthlyAttendanceReportResponse> {
    let params = new HttpParams().set('year', year).set('month', month);
    if (siteIds && siteIds.length > 0) {
      siteIds.forEach(id => { params = params.append('siteIds', id); });
    }
    return this.http.get<ApiEnvelope<MonthlyAttendanceReportResponse>>(`${this.baseUrl}/monthly-report`, { params }).pipe(map(e => e.data));
  }

  /** Same report, rendered as a downloadable file. format: 'xlsx' | 'pdf'. siteIds: empty/undefined = all sites. */
  downloadMonthlyReport(year: number, month: number, format: 'xlsx' | 'pdf', siteIds?: number[]): Observable<Blob> {
    let params = new HttpParams().set('year', year).set('month', month).set('format', format);
    if (siteIds && siteIds.length > 0) {
      siteIds.forEach(id => { params = params.append('siteIds', id); });
    }
    return this.http.get(`${this.baseUrl}/monthly-report/download`, { params, responseType: 'blob' });
  }

  /** Direct edit from the Monthly Report table. paidDaysUsed: null clears the adjustment (reverts to auto-calculated). */
  adjustPaidLeave(employeeId: number, year: number, month: number, paidDaysUsed: number | null): Observable<void> {
    return this.http
      .put<ApiEnvelope<void>>(`${this.baseUrl}/monthly-report/leave-adjustment`, { employeeId, year, month, paidDaysUsed })
      .pipe(map(() => void 0));
  }
}
