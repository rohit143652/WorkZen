import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import { ClientCompanyRequest, ClientCompanyResponse, CompanyFeatureResponse, UpdateCompanyFeaturesRequest } from '../models/client-company.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class ClientCompanyService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/client-companies`;

  list(page = 0, size = 20): Observable<PageResult<ClientCompanyResponse>> {
    return this.http
      .get<ApiEnvelope<PageResult<ClientCompanyResponse>>>(this.baseUrl, { params: { page, size } as any })
      .pipe(map(e => e.data));
  }

  getById(id: number): Observable<ClientCompanyResponse> {
    return this.http.get<ApiEnvelope<ClientCompanyResponse>>(`${this.baseUrl}/${id}`).pipe(map(e => e.data));
  }

  /** Preview of the code the Add form should show (disabled) - the backend generates the same code again on submit if left blank. */
  nextCode(): Observable<string> {
    return this.http.get<ApiEnvelope<{ code: string }>>(`${this.baseUrl}/next-code`).pipe(map(e => e.data.code));
  }

  create(request: ClientCompanyRequest): Observable<ClientCompanyResponse> {
    return this.http.post<ApiEnvelope<ClientCompanyResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  update(id: number, request: ClientCompanyRequest): Observable<ClientCompanyResponse> {
    return this.http.put<ApiEnvelope<ClientCompanyResponse>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  activate(id: number): Observable<ClientCompanyResponse> {
    return this.http.put<ApiEnvelope<ClientCompanyResponse>>(`${this.baseUrl}/${id}/activate`, {}).pipe(map(e => e.data));
  }

  deactivate(id: number): Observable<ClientCompanyResponse> {
    return this.http.put<ApiEnvelope<ClientCompanyResponse>>(`${this.baseUrl}/${id}/deactivate`, {}).pipe(map(e => e.data));
  }

  /** Super Admin "Manage Features" screen - current on/off state for every known feature code, grouped by category. */
  getFeatures(id: number): Observable<CompanyFeatureResponse> {
    return this.http.get<ApiEnvelope<CompanyFeatureResponse>>(`${this.baseUrl}/${id}/features`).pipe(map(e => e.data));
  }

  updateFeatures(id: number, request: UpdateCompanyFeaturesRequest): Observable<Record<string, boolean>> {
    return this.http.put<ApiEnvelope<Record<string, boolean>>>(`${this.baseUrl}/${id}/features`, request).pipe(map(e => e.data));
  }
}
