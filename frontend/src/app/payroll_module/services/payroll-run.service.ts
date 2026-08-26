import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import { PayrollRun, PayrollRunCreateRequest, PayrollRunEmployeeResult } from '../models/payroll-run.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

/**
 * The persisted Payroll Run workflow (architecture refactor Phase 2) - a
 * PayrollRun's numbers are computed once, explicitly, via calculate(), and
 * every subsequent view (get/list/getEmployees) reads only what's already
 * persisted. This is deliberately separate from PayrollService (the
 * EPF/ESI/PT policy settings those numbers are computed from).
 */
@Injectable({ providedIn: 'root' })
export class PayrollRunService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/payroll/runs`;

  create(request: PayrollRunCreateRequest): Observable<PayrollRun> {
    return this.http.post<ApiEnvelope<PayrollRun>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  calculate(id: number): Observable<PayrollRun> {
    return this.http.post<ApiEnvelope<PayrollRun>>(`${this.baseUrl}/${id}/calculate`, {}).pipe(map(e => e.data));
  }

  approve(id: number): Observable<PayrollRun> {
    return this.http.put<ApiEnvelope<PayrollRun>>(`${this.baseUrl}/${id}/approve`, {}).pipe(map(e => e.data));
  }

  markPaid(id: number): Observable<PayrollRun> {
    return this.http.put<ApiEnvelope<PayrollRun>>(`${this.baseUrl}/${id}/mark-paid`, {}).pipe(map(e => e.data));
  }

  cancel(id: number, cancellationReason: string): Observable<PayrollRun> {
    return this.http.put<ApiEnvelope<PayrollRun>>(`${this.baseUrl}/${id}/cancel`, { cancellationReason }).pipe(map(e => e.data));
  }

  /** Controlled reopen of an APPROVED run back to CALCULATED - requires PAYROLL_RUN_REOPEN and a reason. Never works on a PAID run (backend rejects it with a distinct message). */
  reopen(id: number, reopenReason: string): Observable<PayrollRun> {
    return this.http.put<ApiEnvelope<PayrollRun>>(`${this.baseUrl}/${id}/reopen`, { reopenReason }).pipe(map(e => e.data));
  }

  list(year?: number, month?: number, status?: string, page = 0, size = 20): Observable<PageResult<PayrollRun>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (year) params = params.set('year', year);
    if (month) params = params.set('month', month);
    if (status) params = params.set('status', status);
    return this.http.get<ApiEnvelope<PageResult<PayrollRun>>>(this.baseUrl, { params }).pipe(map(e => e.data));
  }

  getById(id: number): Observable<PayrollRun> {
    return this.http.get<ApiEnvelope<PayrollRun>>(`${this.baseUrl}/${id}`).pipe(map(e => e.data));
  }

  getEmployees(id: number, page = 0, size = 200): Observable<PageResult<PayrollRunEmployeeResult>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiEnvelope<PageResult<PayrollRunEmployeeResult>>>(`${this.baseUrl}/${id}/employees`, { params }).pipe(map(e => e.data));
  }

  /** Sets the manual Other Deduction (e.g. Uniform, Canteen, Fine) and Allowance for one employee - takes effect the next time this run is calculated. */
  setEmployeeAdjustment(runId: number, employeeId: number, otherManualDeduction: number, allowance: number): Observable<void> {
    return this.http
      .put<ApiEnvelope<void>>(`${this.baseUrl}/${runId}/employees/${employeeId}/adjustment`, { otherManualDeduction, allowance })
      .pipe(map(() => void 0));
  }

  /** The classic "Salary Register" Excel layout - read-only, built from this run's already-persisted employee results. */
  downloadSalaryRegister(runId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${runId}/export/salary-register`, { responseType: 'blob' });
  }
}
