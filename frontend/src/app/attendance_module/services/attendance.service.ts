import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import {
  AttendanceResponse, AttendanceRuleConfigResponse, BulkMarkAttendanceRequest, BulkMarkAttendanceResult,
  CheckInRequest, CheckOutRequest, CorrectionRequestCreateRequest, CorrectionRequestResponse,
  CorrectionReviewRequest, EmployeeAttendanceOption, MarkAttendanceRequest, MonthlyAttendanceReportResponse,
  TodayAttendanceOverviewResponse, UpdateAttendanceRequest, UpdateAttendanceRuleConfigRequest
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

  /** Today's centralized attendance record for the logged-in employee - check-in/out times,
      working duration, late/early-exit, work mode, source. Null (as data) if nothing recorded yet. */
  today(): Observable<AttendanceResponse | null> {
    return this.http.get<ApiEnvelope<AttendanceResponse | null>>(`${this.baseUrl}/today`).pipe(map(e => e.data));
  }

  checkIn(request: CheckInRequest): Observable<AttendanceResponse> {
    return this.http.post<ApiEnvelope<AttendanceResponse>>(`${this.baseUrl}/check-in`, request).pipe(map(e => e.data));
  }

  checkOut(request: CheckOutRequest): Observable<AttendanceResponse> {
    return this.http.post<ApiEnvelope<AttendanceResponse>>(`${this.baseUrl}/check-out`, request).pipe(map(e => e.data));
  }

  /** Company-wide "who's checked in today" snapshot for the admin dashboard widget. */
  todayOverview(): Observable<TodayAttendanceOverviewResponse> {
    return this.http.get<ApiEnvelope<TodayAttendanceOverviewResponse>>(`${this.baseUrl}/today-overview`).pipe(map(e => e.data));
  }

  /** Company attendance rules (office hours, grace period, thresholds, weekly off) - readable by anyone who can see attendance. */
  getRuleConfig(): Observable<AttendanceRuleConfigResponse> {
    return this.http.get<ApiEnvelope<AttendanceRuleConfigResponse>>(`${this.baseUrl}/rules`).pipe(map(e => e.data));
  }

  updateRuleConfig(request: UpdateAttendanceRuleConfigRequest): Observable<AttendanceRuleConfigResponse> {
    return this.http.put<ApiEnvelope<AttendanceRuleConfigResponse>>(`${this.baseUrl}/rules`, request).pipe(map(e => e.data));
  }

  /** Employee requesting a fix to their own attendance for a given date. */
  createCorrectionRequest(request: CorrectionRequestCreateRequest): Observable<CorrectionRequestResponse> {
    return this.http
      .post<ApiEnvelope<CorrectionRequestResponse>>(`${this.baseUrl}/correction-requests`, request)
      .pipe(map(e => e.data));
  }

  myCorrectionRequests(page: number, size: number): Observable<PageResult<CorrectionRequestResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http
      .get<ApiEnvelope<PageResult<CorrectionRequestResponse>>>(`${this.baseUrl}/correction-requests/mine`, { params })
      .pipe(map(e => e.data));
  }

  /** Admin/HR review queue - status: 'PENDING' | 'APPROVED' | 'REJECTED' | undefined (all). */
  allCorrectionRequests(status: string | undefined, page: number, size: number): Observable<PageResult<CorrectionRequestResponse>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    if (status) params = params.set('status', status);
    return this.http
      .get<ApiEnvelope<PageResult<CorrectionRequestResponse>>>(`${this.baseUrl}/correction-requests`, { params })
      .pipe(map(e => e.data));
  }

  approveCorrectionRequest(id: number, request: CorrectionReviewRequest): Observable<CorrectionRequestResponse> {
    return this.http
      .post<ApiEnvelope<CorrectionRequestResponse>>(`${this.baseUrl}/correction-requests/${id}/approve`, request)
      .pipe(map(e => e.data));
  }

  rejectCorrectionRequest(id: number, request: CorrectionReviewRequest): Observable<CorrectionRequestResponse> {
    return this.http
      .post<ApiEnvelope<CorrectionRequestResponse>>(`${this.baseUrl}/correction-requests/${id}/reject`, request)
      .pipe(map(e => e.data));
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

  /** "My Attendance History" - for anyone with ATTENDANCE_SELF_MARK, even without ATTENDANCE_READ. Always the caller's own records. */
  myHistory(from: string, to: string): Observable<AttendanceResponse[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http
      .get<ApiEnvelope<AttendanceResponse[]>>(`${this.baseUrl}/mine/history`, { params })
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

  /** On-demand selfie fetch - never bundled into list/detail responses (see AttendanceResponse.hasCheckInSelfie/hasCheckOutSelfie). Returns the base64 data-URI, or null if none was captured. */
  getSelfie(attendanceId: number, checkOut: boolean): Observable<string | null> {
    return this.http
      .get<ApiEnvelope<string | null>>(`${this.baseUrl}/${attendanceId}/selfie`, { params: { checkOut } as any })
      .pipe(map(e => e.data));
  }
}
