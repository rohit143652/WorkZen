import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { UserManagementService } from '../../services/user-management.service';
import { EmployeeResponse } from '../../../employee_module/models/employee.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  if (!password || !confirm) return null;
  return password === confirm ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './user-list.component.html'
})
export class UserListComponent {
  private readonly userManagementService = inject(UserManagementService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly users = signal<EmployeeResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly totalElements = signal(0);
  readonly page = signal(0);
  readonly pageSize = 10;
  readonly lastTempPassword = signal<{ userId: number; password: string } | null>(null);

  /** Inline "set a specific password" state - at most one user row open at a time. */
  readonly settingPasswordForId = signal<number | null>(null);
  readonly savingPassword = signal(false);
  readonly setPasswordForm = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: passwordsMatchValidator }
  );

  search = '';

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.userManagementService.list({ search: this.search || undefined, page: this.page(), size: this.pageSize }).subscribe({
      next: res => { this.users.set(res.content); this.totalElements.set(res.totalElements); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); }
    });
  }

  onSearch(): void {
    this.page.set(0);
    this.load();
  }

  totalPages(): number {
    return Math.max(1, Math.ceil(this.totalElements() / this.pageSize));
  }

  goToPage(next: number): void {
    this.page.set(next);
    this.load();
  }

  async unlock(userId: number): Promise<void> {
    const ok = await this.confirmDialog.ask({ title: 'Unlock account?', message: 'This will reset the failed login counter and unlock the account.' });
    if (!ok) return;
    this.userManagementService.unlock(userId).subscribe({
      next: () => { this.toast.success('Account unlocked.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to unlock account.')
    });
  }

  async resetPassword(userId: number): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Reset password?',
      message: 'A new temporary password will be generated and the user will be required to change it on next login.'
    });
    if (!ok) return;
    this.userManagementService.resetPassword(userId).subscribe({
      next: password => { this.lastTempPassword.set({ userId, password }); this.settingPasswordForId.set(null); this.toast.success('Temporary password issued.'); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to reset password.')
    });
  }

  openSetPasswordForm(userId: number): void {
    this.lastTempPassword.set(null);
    this.setPasswordForm.reset();
    this.settingPasswordForId.set(userId);
  }

  cancelSetPassword(): void {
    this.settingPasswordForId.set(null);
  }

  submitSetPassword(userId: number): void {
    if (this.setPasswordForm.invalid) {
      this.setPasswordForm.markAllAsTouched();
      return;
    }
    const { newPassword, confirmPassword } = this.setPasswordForm.getRawValue();
    this.savingPassword.set(true);
    this.userManagementService.setPassword(userId, newPassword, confirmPassword).subscribe({
      next: () => {
        this.toast.success('Password updated. The user must change it on next login.');
        this.savingPassword.set(false);
        this.settingPasswordForId.set(null);
      },
      error: err => {
        this.savingPassword.set(false);
        this.toast.error(err.error?.message ?? 'Unable to update password.');
      }
    });
  }
}
