import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AttendanceService } from '../../services/attendance.service';
import { CORRECTION_REQUEST_TYPES, CorrectionRequestResponse } from '../../models/attendance.model';
import { ToastService } from '../../../shared/services/toast.service';
import { PageResult } from '../../../core/models/page.model';
import { BadgeKind, StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

/**
 * Employee-facing "request a fix" screen: a small form to submit a new request, plus a history
 * of the employee's own past requests and their outcome (PENDING/APPROVED/REJECTED). Submitting
 * never touches the attendance record directly - see AttendanceCorrectionService on the backend.
 */
@Component({
  selector: 'app-correction-request',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StatusBadgeComponent],
  templateUrl: './correction-request.component.html'
})
export class CorrectionRequestComponent {
  private readonly fb = inject(FormBuilder);
  private readonly attendanceService = inject(AttendanceService);
  private readonly toast = inject(ToastService);

  readonly requestTypes = CORRECTION_REQUEST_TYPES;
  readonly submitting = signal(false);
  readonly loadingHistory = signal(true);
  readonly history = signal<PageResult<CorrectionRequestResponse> | null>(null);
  readonly page = signal(0);
  readonly pageSize = 10;

  readonly form = this.fb.nonNullable.group({
    attendanceDate: ['', Validators.required],
    requestType: ['MISSING_CHECK_IN', Validators.required],
    requestedCheckIn: [''],
    requestedCheckOut: [''],
    reason: ['', Validators.required]
  });

  constructor() {
    this.loadHistory();
  }

  loadHistory(): void {
    this.loadingHistory.set(true);
    this.attendanceService.myCorrectionRequests(this.page(), this.pageSize).subscribe({
      next: result => { this.history.set(result); this.loadingHistory.set(false); },
      error: () => this.loadingHistory.set(false)
    });
  }

  goToPage(p: number): void {
    this.page.set(p);
    this.loadHistory();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.submitting.set(true);
    this.attendanceService.createCorrectionRequest({
      attendanceDate: raw.attendanceDate,
      requestType: raw.requestType as any,
      requestedCheckIn: raw.requestedCheckIn ? `${raw.attendanceDate}T${raw.requestedCheckIn}:00` : undefined,
      requestedCheckOut: raw.requestedCheckOut ? `${raw.attendanceDate}T${raw.requestedCheckOut}:00` : undefined,
      reason: raw.reason
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success('Correction request submitted - an admin will review it.');
        this.form.reset({ requestType: 'MISSING_CHECK_IN' });
        this.page.set(0);
        this.loadHistory();
      },
      error: err => {
        this.submitting.set(false);
        this.toast.error(err.error?.message ?? 'Unable to submit the correction request.');
      }
    });
  }

  needsCheckIn(type: string): boolean {
    return type === 'MISSING_CHECK_IN' || type === 'WRONG_CHECK_IN';
  }

  needsCheckOut(type: string): boolean {
    return type === 'MISSING_CHECK_OUT' || type === 'WRONG_CHECK_OUT';
  }

  statusBadgeKind(status: string): BadgeKind {
    if (status === 'APPROVED') return 'success';
    if (status === 'REJECTED') return 'danger';
    return 'warning';
  }
}
