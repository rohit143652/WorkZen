import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { EmployeeService } from '../../services/employee.service';
import { EmployeeResponse } from '../../models/employee.model';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, HasPermissionDirective],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.css'
})
export class EmployeeListComponent {
  private readonly employeeService = inject(EmployeeService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly employees = signal<EmployeeResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly totalElements = signal(0);
  /** Id of the employee whose invitation is being resent right now - disables just that row's button. */
  readonly resendingId = signal<number | null>(null);
  readonly page = signal(0);
  readonly pageSize = 10;

  search = '';
  // Defaults to Active only - an employee whose Full & Final Settlement is done gets
  // deactivated (see ExitService.settle()), so this keeps them out of the default view
  // entirely rather than showing every ex-employee alongside the current team. Switching this
  // dropdown to "Inactive" or "All statuses" still shows them when that's genuinely needed.
  statusFilter = 'ACTIVE';
  loginFilter = '';
  onboardingFilter = '';

  /** Typing triggers a search automatically (see onSearchInput()) instead of needing the
      "Search" button - debounced so a fast typist doesn't fire an API call on every single
      keystroke, only once they've paused for a moment. */
  private readonly searchInput$ = new Subject<string>();

  constructor() {
    this.searchInput$.pipe(debounceTime(350), distinctUntilChanged()).subscribe(() => {
      this.page.set(0);
      this.load();
    });
    this.load();
  }

  onSearchInput(): void {
    this.searchInput$.next(this.search);
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.employeeService
      .search({
        search: this.search || undefined,
        status: this.statusFilter || undefined,
        loginEnabled: this.loginFilter === '' ? undefined : this.loginFilter === 'true',
        onboardingFilter: this.onboardingFilter || undefined,
        page: this.page(),
        size: this.pageSize,
        sort: 'createdAt,desc'
      })
      .subscribe({
        next: res => {
          this.employees.set(res.content);
          this.totalElements.set(res.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        }
      });
  }

  onFilterChange(): void {
    this.page.set(0);
    this.load();
  }

  goToPage(next: number): void {
    this.page.set(next);
    this.load();
  }

  totalPages(): number {
    return Math.max(1, Math.ceil(this.totalElements() / this.pageSize));
  }

  /**
   * An invitation is only outstanding while the account has NOT been activated yet: the employee has
   * a login (username) whose user is still inactive, and onboarding hasn't moved past invited. For
   * anyone who already activated, "Reset Password" is the right tool and the backend refuses a resend.
   */
  canResendInvitation(e: EmployeeResponse): boolean {
    return !!e.username && !e.userActive && (e.onboardingStatus === 'INVITED' || e.onboardingStatus === 'NOT_STARTED');
  }

  async resendInvitation(e: EmployeeResponse): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Resend invitation?',
      message: `Send a new invitation email to ${e.firstName} ${e.lastName} (${e.email})? Any earlier invitation link will stop working.`,
      confirmLabel: 'Resend'
    });
    if (!ok) return;
    this.resendingId.set(e.id);
    this.employeeService.resendInvitation(e.id).subscribe({
      next: sent => {
        this.resendingId.set(null);
        if (sent) {
          this.toast.success(`Invitation sent to ${e.email}.`);
        } else {
          this.toast.warning(`The invitation was regenerated, but the email to ${e.email} could NOT be sent - please check the address.`);
        }
      },
      error: err => { this.resendingId.set(null); this.toast.error(err.error?.message ?? 'Unable to resend the invitation.'); }
    });
  }

  async toggleLogin(employee: EmployeeResponse): Promise<void> {
    if (employee.loginEnabled) {
      const ok = await this.confirmDialog.ask({
        title: 'Disable login access?',
        message: `Are you sure you want to disable login access for ${employee.firstName} ${employee.lastName}?`,
        confirmLabel: 'Disable',
        danger: true
      });
      if (!ok) return;
      this.employeeService.disableLogin(employee.id).subscribe({
        next: () => { this.toast.success('Login access disabled successfully.'); this.load(); },
        error: err => this.toast.error(err.error?.message ?? 'Unable to disable login access.')
      });
    } else {
      this.toast.info('Use "View" to enable login with a username, password and role.');
    }
  }

  async toggleActive(employee: EmployeeResponse): Promise<void> {
    const activating = employee.status !== 'ACTIVE';
    const ok = await this.confirmDialog.ask({
      title: activating ? 'Activate employee?' : 'Deactivate employee?',
      message: activating
        ? `Reactivate ${employee.firstName} ${employee.lastName}?`
        : `Deactivating will also disable their login access, if any. Continue?`,
      confirmLabel: activating ? 'Activate' : 'Deactivate',
      danger: !activating
    });
    if (!ok) return;

    const action$ = activating ? this.employeeService.activate(employee.id) : this.employeeService.deactivate(employee.id);
    action$.subscribe({
      next: () => { this.toast.success(activating ? 'Employee activated successfully.' : 'Employee deactivated successfully.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to update employee.')
    });
  }
}
