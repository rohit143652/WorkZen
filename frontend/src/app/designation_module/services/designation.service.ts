import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DesignationRequest, DesignationResponse } from '../models/designation.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class DesignationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/designations`;

  list(includeInactive = false): Observable<DesignationResponse[]> {
    const params = new HttpParams().set('includeInactive', includeInactive);
    return this.http.get<ApiEnvelope<DesignationResponse[]>>(this.baseUrl, { params }).pipe(map(e => e.data));
  }

  create(request: DesignationRequest): Observable<DesignationResponse> {
    return this.http.post<ApiEnvelope<DesignationResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  rename(id: number, request: DesignationRequest): Observable<DesignationResponse> {
    return this.http.put<ApiEnvelope<DesignationResponse>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  activate(id: number): Observable<DesignationResponse> {
    return this.http.put<ApiEnvelope<DesignationResponse>>(`${this.baseUrl}/${id}/activate`, {}).pipe(map(e => e.data));
  }

  deactivate(id: number): Observable<DesignationResponse> {
    return this.http.put<ApiEnvelope<DesignationResponse>>(`${this.baseUrl}/${id}/deactivate`, {}).pipe(map(e => e.data));
  }
}
