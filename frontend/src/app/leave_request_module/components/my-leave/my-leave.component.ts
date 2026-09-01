import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LeaveRequestService } from '../../services/leave-request.service';
import { LeaveRequestResponse } from '../../models/leave-request.model';
import { ToastService } from '../../../shared/services/toast.service';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-my-leave',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './my-leave.component.html'
})
export class MyLeaveComponent {
  private readonly leaveRequestService = inject(LeaveRequestService);
  private readonly toast = inject(ToastService);

  readonly requests = signal<LeaveRequestResponse[]>([]);
  readonly loading = signal(true);
  readonly showAddForm = signal(false);
  readonly saving = signal(false);

  newStartDate = '';
  newEndDate = '';
  newReason = '';

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.leaveRequestService.findMine().subscribe({
      next: list => { this.requests.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openAddForm(): void {
    this.showAddForm.set(true);
    this.newStartDate = '';
    this.newEndDate = '';
    this.newReason = '';
  }

  closeAddForm(): void {
    this.showAddForm.set(false);
  }

  submit(): void {
    if (!this.newStartDate || !this.newEndDate) return;
    if (this.newEndDate < this.newStartDate) {
      this.toast.error('End date cannot be before start date.');
      return;
    }
    this.saving.set(true);
    this.leaveRequestService.selfCreate({
      startDate: this.newStartDate,
      endDate: this.newEndDate,
      reason: this.newReason.trim() || undefined
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showAddForm.set(false);
        this.toast.success('Leave request submitted - awaiting approval.');
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to submit this leave request.');
      }
    });
  }
}
