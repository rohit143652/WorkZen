import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RoleOption } from '../models/role.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

export interface RoleRequest {
  name: string;
  description?: string;
  permissionIds: number[];
}

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/roles`;

  /** Roles are always loaded from the backend - never hardcoded in Angular. */
  list(): Observable<RoleOption[]> {
    return this.http.get<ApiEnvelope<RoleOption[]>>(this.baseUrl).pipe(map(e => e.data));
  }

  create(request: RoleRequest): Observable<RoleOption> {
    return this.http.post<ApiEnvelope<RoleOption>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  /** Updates name/description AND permissions together in one call - matches PUT /api/roles/{id}, which the backend already treats as a single "replace with this" operation. */
  update(id: number, request: RoleRequest): Observable<RoleOption> {
    return this.http.put<ApiEnvelope<RoleOption>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }
}
