import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LeaveRequestAdminCreateRequest, LeaveRequestCreateRequest,
  LeaveRequestResponse, LeaveRequestReviewRequest
} from '../models/leave-request.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class LeaveRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/leave-requests`;

  /** Every leave request for the tenant - admin view. */
  findAll(): Observable<LeaveRequestResponse[]> {
    return this.http.get<ApiEnvelope<LeaveRequestResponse[]>>(this.baseUrl).pipe(map(e => e.data));
  }

  /** The logged-in employee's own leave requests. */
  findMine(): Observable<LeaveRequestResponse[]> {
    return this.http.get<ApiEnvelope<LeaveRequestResponse[]>>(`${this.baseUrl}/mine`).pipe(map(e => e.data));
  }

  /** Self-service apply - starts PENDING, needs admin/supervisor approval. */
  selfCreate(request: LeaveRequestCreateRequest): Observable<LeaveRequestResponse> {
    return this.http.post<ApiEnvelope<LeaveRequestResponse>>(`${this.baseUrl}/mine`, request).pipe(map(e => e.data));
  }

  /** Admin/supervisor adds leave directly for any employee (with or without login) - goes straight to APPROVED. */
  adminCreate(request: LeaveRequestAdminCreateRequest): Observable<LeaveRequestResponse> {
    return this.http.post<ApiEnvelope<LeaveRequestResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  approve(id: number, request: LeaveRequestReviewRequest = {}): Observable<LeaveRequestResponse> {
    return this.http.put<ApiEnvelope<LeaveRequestResponse>>(`${this.baseUrl}/${id}/approve`, request).pipe(map(e => e.data));
  }

  reject(id: number, request: LeaveRequestReviewRequest = {}): Observable<LeaveRequestResponse> {
    return this.http.put<ApiEnvelope<LeaveRequestResponse>>(`${this.baseUrl}/${id}/reject`, request).pipe(map(e => e.data));
  }
}
