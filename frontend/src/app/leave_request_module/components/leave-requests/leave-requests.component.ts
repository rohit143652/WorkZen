import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LeaveRequestService } from '../../services/leave-request.service';
import { LeaveRequestResponse } from '../../models/leave-request.model';
import { EmployeeService } from '../../../employee_module/services/employee.service';
import { EmployeeResponse } from '../../../employee_module/models/employee.model';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { EmployeeSearchSelectComponent } from '../../../employee_module/components/employee-search-select/employee-search-select.component';

@Component({
  selector: 'app-leave-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, HasPermissionDirective, StatusBadgeComponent, EmployeeSearchSelectComponent],
  templateUrl: './leave-requests.component.html'
})
export class LeaveRequestsComponent {
  private readonly leaveRequestService = inject(LeaveRequestService);
  private readonly employeeService = inject(EmployeeService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly requests = signal<LeaveRequestResponse[]>([]);
  readonly activeEmployees = signal<EmployeeResponse[]>([]);
  readonly loading = signal(true);

  readonly showAddForm = signal(false);
  readonly saving = signal(false);
  newEmployeeId: number | null = null;
  newStartDate = '';
  newEndDate = '';
  newReason = '';

  readonly actingOnId = signal<number | null>(null);

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.leaveRequestService.findAll().subscribe({
      next: list => { this.requests.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openAddForm(): void {
    this.showAddForm.set(true);
    this.newEmployeeId = null;
    this.newStartDate = '';
    this.newEndDate = '';
    this.newReason = '';
    if (this.activeEmployees().length === 0) {
      this.employeeService.search({ status: 'ACTIVE', size: 500, sort: 'employeeCode,asc' }).subscribe(page => {
        this.activeEmployees.set(page.content);
      });
    }
  }

  closeAddForm(): void {
    this.showAddForm.set(false);
  }

  submitAdminAdd(): void {
    if (!this.newEmployeeId || !this.newStartDate || !this.newEndDate) return;
    if (this.newEndDate < this.newStartDate) {
      this.toast.error('End date cannot be before start date.');
      return;
    }
    this.saving.set(true);
    this.leaveRequestService.adminCreate({
      employeeId: this.newEmployeeId,
      startDate: this.newStartDate,
      endDate: this.newEndDate,
      reason: this.newReason.trim() || undefined
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showAddForm.set(false);
        this.toast.success('Leave added and marked on attendance.');
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to add this leave.');
      }
    });
  }

  approve(request: LeaveRequestResponse): void {
    this.actingOnId.set(request.id);
    this.leaveRequestService.approve(request.id).subscribe({
      next: () => {
        this.actingOnId.set(null);
        this.toast.success(`Approved - ${request.employeeName}'s attendance has been marked On Leave for these dates.`);
        this.load();
      },
      error: err => {
        this.actingOnId.set(null);
        this.toast.error(err.error?.message ?? 'Unable to approve this request.');
      }
    });
  }

  async reject(request: LeaveRequestResponse): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Reject this leave request?',
      message: `Reject ${request.employeeName}'s leave request for ${request.startDate} to ${request.endDate}? No attendance changes will be made.`,
      confirmLabel: 'Reject Request'
    });
    if (!ok) return;

    this.actingOnId.set(request.id);
    this.leaveRequestService.reject(request.id).subscribe({
      next: () => {
        this.actingOnId.set(null);
        this.toast.success('Leave request rejected.');
        this.load();
      },
      error: err => {
        this.actingOnId.set(null);
        this.toast.error(err.error?.message ?? 'Unable to reject this request.');
      }
    });
  }
}
