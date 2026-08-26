import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import { SiteRequest, SiteResponse } from '../models/site.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class SiteService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/sites`;

  list(page = 0, size = 50): Observable<PageResult<SiteResponse>> {
    return this.http
      .get<ApiEnvelope<PageResult<SiteResponse>>>(this.baseUrl, { params: { page, size } as any })
      .pipe(map(e => e.data));
  }

  getById(id: number): Observable<SiteResponse> {
    return this.http.get<ApiEnvelope<SiteResponse>>(`${this.baseUrl}/${id}`).pipe(map(e => e.data));
  }

  /** Preview of the code the Add form should show (disabled) - the backend generates the same code again on submit if left blank. */
  nextCode(): Observable<string> {
    return this.http.get<ApiEnvelope<{ code: string }>>(`${this.baseUrl}/next-code`).pipe(map(e => e.data.code));
  }

  create(request: SiteRequest): Observable<SiteResponse> {
    return this.http.post<ApiEnvelope<SiteResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  update(id: number, request: SiteRequest): Observable<SiteResponse> {
    return this.http.put<ApiEnvelope<SiteResponse>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  activate(id: number): Observable<SiteResponse> {
    return this.http.put<ApiEnvelope<SiteResponse>>(`${this.baseUrl}/${id}/activate`, {}).pipe(map(e => e.data));
  }

  deactivate(id: number): Observable<SiteResponse> {
    return this.http.put<ApiEnvelope<SiteResponse>>(`${this.baseUrl}/${id}/deactivate`, {}).pipe(map(e => e.data));
  }

  employeesAtSite(siteId: number): Observable<any[]> {
    return this.http
      .get<ApiEnvelope<any[]>>(`${environment.apiUrl}/sites/${siteId}/employees`)
      .pipe(map(e => e.data));
  }
}
