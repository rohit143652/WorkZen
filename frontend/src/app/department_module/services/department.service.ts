import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DepartmentRequest, DepartmentResponse } from '../models/department.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/departments`;

  list(includeInactive = false): Observable<DepartmentResponse[]> {
    const params = new HttpParams().set('includeInactive', includeInactive);
    return this.http.get<ApiEnvelope<DepartmentResponse[]>>(this.baseUrl, { params }).pipe(map(e => e.data));
  }

  create(request: DepartmentRequest): Observable<DepartmentResponse> {
    return this.http.post<ApiEnvelope<DepartmentResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  rename(id: number, request: DepartmentRequest): Observable<DepartmentResponse> {
    return this.http.put<ApiEnvelope<DepartmentResponse>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  activate(id: number): Observable<DepartmentResponse> {
    return this.http.put<ApiEnvelope<DepartmentResponse>>(`${this.baseUrl}/${id}/activate`, {}).pipe(map(e => e.data));
  }

  deactivate(id: number): Observable<DepartmentResponse> {
    return this.http.put<ApiEnvelope<DepartmentResponse>>(`${this.baseUrl}/${id}/deactivate`, {}).pipe(map(e => e.data));
  }
}
