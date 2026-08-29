import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EmployeeExitRequest, EmployeeExitResponse } from '../models/employee-exit.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class ExitService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/employee-exits`;

  list(): Observable<EmployeeExitResponse[]> {
    return this.http.get<ApiEnvelope<EmployeeExitResponse[]>>(this.baseUrl).pipe(map(e => e.data));
  }

  initiate(request: EmployeeExitRequest): Observable<EmployeeExitResponse> {
    return this.http.post<ApiEnvelope<EmployeeExitResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  previewSettlement(id: number): Observable<EmployeeExitResponse> {
    return this.http.get<ApiEnvelope<EmployeeExitResponse>>(`${this.baseUrl}/${id}/settlement-preview`).pipe(map(e => e.data));
  }

  settle(id: number): Observable<EmployeeExitResponse> {
    return this.http.post<ApiEnvelope<EmployeeExitResponse>>(`${this.baseUrl}/${id}/settle`, {}).pipe(map(e => e.data));
  }
}
