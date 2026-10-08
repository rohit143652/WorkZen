import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const next = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return !next || !confirm || next === confirm ? null : { mismatch: true };
}

function differsFromCurrent(control: AbstractControl): ValidationErrors | null {
  const current = control.get('currentPassword')?.value;
  const next = control.get('newPassword')?.value;
  return !current || !next || current !== next ? null : { sameAsCurrent: true };
}

/**
 * "Change Password" for whoever is logged in - an employee, a Client Admin, a Super Admin, any user - shown on
 * My Profile. Enter the current password, then the new one twice (the second entry verifies it). The server
 * re-checks everything (current password correct, new one different, the two entries equal) and ends every
 * session, so afterwards the person signs in again with the new password - the same behaviour as the forced
 * change-password screen, which is left untouched.
 */
@Component({
  selector: 'app-change-password-card',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password-card.component.html'
})
export class ChangePasswordCardComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly expanded = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly succeeded = signal(false);
  readonly showCurrent = signal(false);
  readonly showNew = signal(false);
  readonly showConfirm = signal(false);

  readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: [passwordsMatch, differsFromCurrent] }
  );

  toggle(): void {
    if (this.saving() || this.succeeded()) return;
    if (this.expanded()) {
      this.form.reset();
      this.errorMessage.set(null);
      this.showCurrent.set(false);
      this.showNew.set(false);
      this.showConfirm.set(false);
    }
    this.expanded.update(open => !open);
  }

  submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const { currentPassword, newPassword, confirmPassword } = this.form.getRawValue();
    this.authService.changePassword(currentPassword, newPassword, confirmPassword).subscribe({
      next: () => {
        this.saving.set(false);
        this.succeeded.set(true);
        // Every session has ended (the server revoked them), so show the confirmation for a moment, then go to sign-in.
        setTimeout(() => this.router.navigateByUrl('/login'), 1800);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.errorMessage.set(err.error?.message ?? 'Unable to change the password. Please try again.');
      }
    });
  }
}
