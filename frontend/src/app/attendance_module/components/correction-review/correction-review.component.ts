import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AttendanceService } from '../../services/attendance.service';
import { CorrectionRequestResponse } from '../../models/attendance.model';
import { ToastService } from '../../../shared/services/toast.service';
import { PageResult } from '../../../core/models/page.model';
import { BadgeKind, StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

/**
 * Admin/HR review queue for attendance correction requests. Approving/rejecting always goes
 * through AttendanceCorrectionService on the backend - this component only ever calls the
 * approve/reject endpoints, it never edits an attendance record directly.
 */
@Component({
  selector: 'app-correction-review',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './correction-review.component.html'
})
export class CorrectionReviewComponent {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toast = inject(ToastService);

  readonly statusFilter = signal<string>('PENDING');
  readonly loading = signal(true);
  readonly result = signal<PageResult<CorrectionRequestResponse> | null>(null);
  readonly page = signal(0);
  readonly pageSize = 10;
  readonly actingOnId = signal<number | null>(null);
  readonly remarksById = signal<Record<number, string>>({});

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.attendanceService.allCorrectionRequests(this.statusFilter() || undefined, this.page(), this.pageSize).subscribe({
      next: r => { this.result.set(r); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  onFilterChange(status: string): void {
    this.statusFilter.set(status);
    this.page.set(0);
    this.load();
  }

  goToPage(p: number): void {
    this.page.set(p);
    this.load();
  }

  remarksFor(id: number): string {
    return this.remarksById()[id] ?? '';
  }

  setRemarks(id: number, value: string): void {
    this.remarksById.update(m => ({ ...m, [id]: value }));
  }

  approve(r: CorrectionRequestResponse): void {
    this.actingOnId.set(r.id);
    this.attendanceService.approveCorrectionRequest(r.id, { remarks: this.remarksFor(r.id) }).subscribe({
      next: () => {
        this.actingOnId.set(null);
        this.toast.success('Correction approved - the attendance record has been updated.');
        this.load();
      },
      error: err => {
        this.actingOnId.set(null);
        this.toast.error(err.error?.message ?? 'Unable to approve this request.');
      }
    });
  }

  reject(r: CorrectionRequestResponse): void {
    this.actingOnId.set(r.id);
    this.attendanceService.rejectCorrectionRequest(r.id, { remarks: this.remarksFor(r.id) }).subscribe({
      next: () => {
        this.actingOnId.set(null);
        this.toast.success('Correction request rejected.');
        this.load();
      },
      error: err => {
        this.actingOnId.set(null);
        this.toast.error(err.error?.message ?? 'Unable to reject this request.');
      }
    });
  }

  statusBadgeKind(status: string): BadgeKind {
    if (status === 'APPROVED') return 'success';
    if (status === 'REJECTED') return 'danger';
    return 'warning';
  }
}
