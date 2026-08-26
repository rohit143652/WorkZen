import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly showPassword = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly accountLocked = signal(false);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
    rememberMe: [false]
  });

  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }

  submit(): void {
    this.errorMessage.set(null);
    this.accountLocked.set(false);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.authService.login(this.form.getRawValue()).subscribe({
      next: response => {
        this.loading.set(false);
        if (response.user.mustChangePassword) {
          this.router.navigateByUrl('/change-password');
          return;
        }
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.accountLocked.set(true);
          this.errorMessage.set(err.error?.message ?? 'Account is locked. Please contact an administrator.');
        } else if (err.status === 401) {
          this.errorMessage.set(err.error?.message ?? 'Invalid username or password');
        } else if (err.status === 0) {
          this.errorMessage.set('Unable to reach the server. Please try again later.');
        } else {
          this.errorMessage.set(err.error?.message ?? 'An unexpected error occurred. Please try again.');
        }
      }
    });
  }
}
