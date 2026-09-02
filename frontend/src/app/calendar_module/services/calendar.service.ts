import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CalendarItemResponse, EventRequest } from '../models/calendar.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class CalendarService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/calendar`;

  /** start/end as ISO local date-time strings (no timezone suffix) - matches the backend's LocalDateTime params exactly, so no UTC-shift surprises (see business rule #14). */
  findInRange(start: string, end: string): Observable<CalendarItemResponse[]> {
    return this.http.get<ApiEnvelope<CalendarItemResponse[]>>(this.baseUrl, { params: { start, end } }).pipe(map(e => e.data));
  }
}

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/events`;

  create(request: EventRequest): Observable<CalendarItemResponse> {
    return this.http.post<ApiEnvelope<CalendarItemResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  update(id: number, request: EventRequest): Observable<CalendarItemResponse> {
    return this.http.put<ApiEnvelope<CalendarItemResponse>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiEnvelope<void>>(`${this.baseUrl}/${id}`).pipe(map(() => undefined));
  }
}
