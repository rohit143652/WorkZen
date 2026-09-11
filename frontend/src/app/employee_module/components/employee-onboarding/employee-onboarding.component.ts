import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EmployeeOnboardingService, OnboardingValidateResponse } from '../../services/employee-onboarding.service';

/**
 * The public onboarding landing page (/employee-onboarding/:token) - no login required to reach
 * this, since setting up the FIRST login is the entire point (see backend
 * EmployeeOnboardingController/SecurityConfig.PUBLIC_ENDPOINTS).
 *
 * Three steps, all on one page rather than three separate routes: validate the token (on load),
 * enter the one-time code, then choose a password. Keeping it one component avoids re-fetching
 * validate() on every step and keeps the token/code available across steps without stashing them
 * in a service just for this one short-lived flow.
 */
@Component({
  selector: 'app-employee-onboarding',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './employee-onboarding.component.html'
})
export class EmployeeOnboardingComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly onboardingService = inject(EmployeeOnboardingService);

  private readonly token = this.route.snapshot.paramMap.get('token') ?? '';

  readonly loading = signal(true);
  readonly info = signal<OnboardingValidateResponse | null>(null);
  readonly loadError = signal<string | null>(null);

  /** 'code' -> enter the one-time verification code; 'password' -> code verified, choose a password; 'done' -> success. */
  readonly step = signal<'code' | 'password' | 'done'>('code');
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  code = '';
  password = '';
  confirmPassword = '';

  constructor() {
    if (!this.token) {
      this.loadError.set('This onboarding link is invalid.');
      this.loading.set(false);
      return;
    }
    this.onboardingService.validate(this.token).subscribe({
      next: info => {
        this.info.set(info);
        this.loading.set(false);
        if (info.alreadyUsed) {
          this.loadError.set('This invitation has already been used. If you already set your password, please log in instead.');
        } else if (info.expired) {
          this.loadError.set('This invitation has expired. Please ask your admin to resend it.');
        }
      },
      error: () => {
        this.loadError.set('This onboarding link is invalid or no longer active.');
        this.loading.set(false);
      }
    });
  }

  submitCode(): void {
    if (!this.code || this.code.trim().length === 0) {
      this.errorMessage.set('Please enter the verification code from your email.');
      return;
    }
    this.errorMessage.set(null);
    this.submitting.set(true);
    this.onboardingService.verifyCode(this.token, this.code.trim()).subscribe({
      next: () => { this.submitting.set(false); this.step.set('password'); },
      error: err => { this.submitting.set(false); this.errorMessage.set(err.error?.message ?? 'Incorrect verification code.'); }
    });
  }

  submitPassword(): void {
    if (!this.password || this.password.length < 8) {
      this.errorMessage.set('Password must be at least 8 characters.');
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.errorMessage.set('Passwords do not match.');
      return;
    }
    this.errorMessage.set(null);
    this.submitting.set(true);
    this.onboardingService.setPassword(this.token, this.code.trim(), this.password, this.confirmPassword).subscribe({
      next: () => { this.submitting.set(false); this.step.set('done'); },
      error: err => { this.submitting.set(false); this.errorMessage.set(err.error?.message ?? 'Unable to set your password. Please try again.'); }
    });
  }

  goToLogin(): void {
    this.router.navigateByUrl('/login');
  }
}
