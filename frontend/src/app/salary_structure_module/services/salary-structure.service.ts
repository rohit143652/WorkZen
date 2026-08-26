import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResult } from '../../core/models/page.model';
import { SalaryStructureRequest, SalaryStructureResponse } from '../models/salary-structure.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class SalaryStructureService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/salary-structures`;

  list(page: number, size: number): Observable<PageResult<SalaryStructureResponse>> {
    return this.http
      .get<ApiEnvelope<PageResult<SalaryStructureResponse>>>(this.baseUrl, { params: { page, size } })
      .pipe(map(e => e.data));
  }

  /** Unpaged, ACTIVE-only list for pickers (e.g. the Employee form's Salary Structure dropdown). */
  listActive(): Observable<SalaryStructureResponse[]> {
    return this.http.get<ApiEnvelope<SalaryStructureResponse[]>>(`${this.baseUrl}/active`).pipe(map(e => e.data));
  }

  getById(id: number): Observable<SalaryStructureResponse> {
    return this.http.get<ApiEnvelope<SalaryStructureResponse>>(`${this.baseUrl}/${id}`).pipe(map(e => e.data));
  }

  /** Preview of the code the Add form should show (disabled) - the backend generates the same code again on submit if left blank. */
  nextCode(): Observable<string> {
    return this.http.get<ApiEnvelope<{ code: string }>>(`${this.baseUrl}/next-code`).pipe(map(e => e.data.code));
  }

  create(request: SalaryStructureRequest): Observable<SalaryStructureResponse> {
    return this.http.post<ApiEnvelope<SalaryStructureResponse>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  update(id: number, request: SalaryStructureRequest): Observable<SalaryStructureResponse> {
    return this.http.put<ApiEnvelope<SalaryStructureResponse>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  activate(id: number): Observable<SalaryStructureResponse> {
    return this.http.put<ApiEnvelope<SalaryStructureResponse>>(`${this.baseUrl}/${id}/activate`, {}).pipe(map(e => e.data));
  }

  deactivate(id: number): Observable<SalaryStructureResponse> {
    return this.http.put<ApiEnvelope<SalaryStructureResponse>>(`${this.baseUrl}/${id}/deactivate`, {}).pipe(map(e => e.data));
  }

  duplicate(id: number): Observable<SalaryStructureResponse> {
    return this.http.post<ApiEnvelope<SalaryStructureResponse>>(`${this.baseUrl}/${id}/duplicate`, {}).pipe(map(e => e.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiEnvelope<void>>(`${this.baseUrl}/${id}`).pipe(map(() => void 0));
  }
}
