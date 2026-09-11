import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

export interface OnboardingValidateResponse {
  employeeName: string;
  employeeCode: string;
  companyName: string;
  expired: boolean;
  alreadyUsed: boolean;
}

/** Public (unauthenticated) service for the employee onboarding landing page - see backend EmployeeOnboardingController for why these three calls need no JWT. */
@Injectable({ providedIn: 'root' })
export class EmployeeOnboardingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/onboarding`;

  validate(token: string): Observable<OnboardingValidateResponse> {
    return this.http.get<ApiEnvelope<OnboardingValidateResponse>>(`${this.baseUrl}/validate/${token}`).pipe(map(e => e.data));
  }

  verifyCode(token: string, code: string): Observable<void> {
    return this.http.post<ApiEnvelope<void>>(`${this.baseUrl}/verify-code`, { token, code }).pipe(map(() => void 0));
  }

  setPassword(token: string, verificationCode: string, password: string, confirmPassword: string): Observable<void> {
    return this.http.post<ApiEnvelope<void>>(`${this.baseUrl}/set-password`, { token, verificationCode, password, confirmPassword })
      .pipe(map(() => void 0));
  }
}
