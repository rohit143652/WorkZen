import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import { ClientCompanyRequest, ClientCompanyResponse } from '../models/client-company.model';

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
}
