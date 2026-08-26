import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SalaryComponentRequest, SalaryComponentResponse } from '../models/salary-component.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class SalaryComponentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/salary-components`;

  list(includeInactive = false): Observable<SalaryComponentResponse[]> {
    const params = new HttpParams().set('includeInactive', includeInactive);
    return this.http.get<ApiEnvelope<SalaryComponentResponse[]>>(this.baseUrl, { params }).pipe(map(e => e.data));
  }

  create(request: SalaryComponentRequest): Observable<SalaryComponentResponse> {
    return this.http.post<ApiEnvelope<SalaryComponentResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  update(id: number, request: SalaryComponentRequest): Observable<SalaryComponentResponse> {
    return this.http.put<ApiEnvelope<SalaryComponentResponse>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  activate(id: number): Observable<SalaryComponentResponse> {
    return this.http.put<ApiEnvelope<SalaryComponentResponse>>(`${this.baseUrl}/${id}/activate`, {}).pipe(map(e => e.data));
  }

  deactivate(id: number): Observable<SalaryComponentResponse> {
    return this.http.put<ApiEnvelope<SalaryComponentResponse>>(`${this.baseUrl}/${id}/deactivate`, {}).pipe(map(e => e.data));
  }
}
