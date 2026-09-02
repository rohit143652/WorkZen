import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HolidayRequest, HolidayResponse } from '../models/holiday.model';

export interface HolidayBulkResult {
  totalRequested: number;
  successCount: number;
  failureCount: number;
  errors: { name: string; reason: string }[];
}

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class HolidayService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/holidays`;

  list(): Observable<HolidayResponse[]> {
    return this.http.get<ApiEnvelope<HolidayResponse[]>>(this.baseUrl).pipe(map(e => e.data));
  }

  create(request: HolidayRequest): Observable<HolidayResponse> {
    return this.http.post<ApiEnvelope<HolidayResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  bulkCreate(requests: HolidayRequest[]): Observable<HolidayBulkResult> {
    return this.http.post<ApiEnvelope<HolidayBulkResult>>(`${this.baseUrl}/bulk`, requests).pipe(map(e => e.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiEnvelope<void>>(`${this.baseUrl}/${id}`).pipe(map(() => void 0));
  }
}
