import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import {
  BulkAssignmentResult, BulkEmployeeAssignmentRequest, BulkEndAssignmentRequest, BulkEndResult,
  EmployeeAssignmentRequest, EmployeeAssignmentResponse, TransferEmployeeRequest
} from '../models/employee-assignment.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class EmployeeAssignmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/employee-assignments`;

  list(page = 0, size = 20): Observable<PageResult<EmployeeAssignmentResponse>> {
    return this.http
      .get<ApiEnvelope<PageResult<EmployeeAssignmentResponse>>>(this.baseUrl, { params: { page, size } as any })
      .pipe(map(e => e.data));
  }

  assign(request: EmployeeAssignmentRequest): Observable<EmployeeAssignmentResponse> {
    return this.http.post<ApiEnvelope<EmployeeAssignmentResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  bulkAssign(request: BulkEmployeeAssignmentRequest): Observable<BulkAssignmentResult> {
    return this.http
      .post<ApiEnvelope<BulkAssignmentResult>>(`${this.baseUrl}/bulk`, request)
      .pipe(map(e => e.data));
  }

  endAssignment(id: number): Observable<EmployeeAssignmentResponse> {
    return this.http
      .post<ApiEnvelope<EmployeeAssignmentResponse>>(`${this.baseUrl}/${id}/end`, {})
      .pipe(map(e => e.data));
  }

  /** All ACTIVE assignments across the current tenant - used to disable already-assigned
   *  employees in the assignment picker, regardless of which site they're currently at. */
  listActive(): Observable<EmployeeAssignmentResponse[]> {
    return this.http
      .get<ApiEnvelope<EmployeeAssignmentResponse[]>>(`${this.baseUrl}/active`)
      .pipe(map(e => e.data));
  }

  bulkEnd(request: BulkEndAssignmentRequest): Observable<BulkEndResult> {
    return this.http
      .post<ApiEnvelope<BulkEndResult>>(`${this.baseUrl}/bulk-end`, request)
      .pipe(map(e => e.data));
  }

  transfer(employeeId: number, request: TransferEmployeeRequest): Observable<EmployeeAssignmentResponse> {
    return this.http
      .post<ApiEnvelope<EmployeeAssignmentResponse>>(`${environment.apiUrl}/employees/${employeeId}/transfer`, request)
      .pipe(map(e => e.data));
  }

  historyForEmployee(employeeId: number): Observable<EmployeeAssignmentResponse[]> {
    return this.http
      .get<ApiEnvelope<EmployeeAssignmentResponse[]>>(`${environment.apiUrl}/employees/${employeeId}/assignments`)
      .pipe(map(e => e.data));
  }
}
