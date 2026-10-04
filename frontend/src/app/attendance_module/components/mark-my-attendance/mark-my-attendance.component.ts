import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Component, inject, signal } from '@angular/core';
import { AttendanceService } from '../../services/attendance.service';
import { AttendanceResponse } from '../../models/attendance.model';
import { ToastService } from '../../../shared/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { BadgeKind, StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

/**
 * "My Attendance History" - the employee's own past attendance records, including any captured
 * selfies (viewed on demand, never loaded into the list itself).
 *
 * Check-in/check-out itself now lives ENTIRELY on the Dashboard's one-click quick-action card
 * (including the full camera-capture flow when a selfie is required) - kept there deliberately
 * so daily attendance marking stays the single click it's meant to be, rather than needing a
 * separate page visit. This page's job is purely to look back at what's already been recorded.
 */
@Component({
  selector: 'app-mark-my-attendance',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent],
  templateUrl: './mark-my-attendance.component.html',
  styleUrl: './mark-my-attendance.component.css'
})
export class MarkMyAttendanceComponent {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toast = inject(ToastService);
  readonly authState = inject(AuthStateService);

  readonly loading = signal(true);
  readonly history = signal<AttendanceResponse[]>([]);
  readonly viewingSelfie = signal<string | null>(null);
  readonly loadingSelfie = signal(false);

  constructor() {
    this.loadHistory();
  }

  private loadHistory(): void {
    this.loading.set(true);
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 30);
    const iso = (d: Date) => d.toISOString().slice(0, 10);
    this.attendanceService.myHistory(iso(from), iso(to)).subscribe({
      next: records => { this.history.set(records); this.loading.set(false); },
      error: () => { this.toast.error('Unable to load your attendance history.'); this.loading.set(false); }
    });
  }

  formatMinutes(totalMinutes?: number): string {
    if (totalMinutes == null) return '-';
    const h = Math.floor(totalMinutes / 60);
    const m = totalMinutes % 60;
    return `${h}h ${m}m`;
  }

  viewSelfie(attendanceId: number, checkOut: boolean): void {
    this.loadingSelfie.set(true);
    this.attendanceService.getSelfie(attendanceId, checkOut).subscribe({
      next: data => { this.viewingSelfie.set(data); this.loadingSelfie.set(false); },
      error: () => { this.toast.error('Unable to load this photo.'); this.loadingSelfie.set(false); }
    });
  }

  historyBadgeKind(status: string): BadgeKind {
    switch (status) {
      case 'PRESENT': return 'success';
      case 'ABSENT': return 'danger';
      case 'HALF_DAY': return 'warning';
      case 'ON_LEAVE': return 'info';
      case 'WORKING': return 'info';
      default: return 'muted';
    }
  }
}
