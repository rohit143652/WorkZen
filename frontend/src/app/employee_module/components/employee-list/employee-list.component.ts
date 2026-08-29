import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
  readonly page = signal(0);
  readonly pageSize = 10;

  search = '';
  statusFilter = '';
  loginFilter = '';

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.employeeService
      .search({
        search: this.search || undefined,
        status: this.statusFilter || undefined,
        loginEnabled: this.loginFilter === '' ? undefined : this.loginFilter === 'true',
        page: this.page(),
        size: this.pageSize,
        sort: 'lastName,asc'
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
