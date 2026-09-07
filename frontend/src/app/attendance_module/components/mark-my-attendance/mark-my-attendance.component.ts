import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Component, OnDestroy, inject, signal, computed } from '@angular/core';
import { AttendanceService } from '../../services/attendance.service';
import { AttendanceResponse, WORK_MODES, WorkMode } from '../../models/attendance.model';
import { ToastService } from '../../../shared/services/toast.service';
import { FeatureStateService } from '../../../core/services/feature-state.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { BadgeKind, StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

/**
 * "Today's Attendance" - the employee's centralized check-in/check-out card. This is the new
 * primary flow (see AttendanceService.checkIn()/checkOut() on the backend); the OLD one-click
 * "mark myself Present, no times" flow (markMine()) is kept available as a secondary option
 * below it for anyone who genuinely doesn't want time-tracking for a given day - both write to
 * the exact same centralized attendance row, never two separate records.
 */
@Component({
  selector: 'app-mark-my-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusBadgeComponent],
  templateUrl: './mark-my-attendance.component.html',
  styleUrl: './mark-my-attendance.component.css'
})
export class MarkMyAttendanceComponent implements OnDestroy {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toast = inject(ToastService);
  private readonly featureState = inject(FeatureStateService);
  readonly authState = inject(AuthStateService);

  /** Whether this account can reach the separate, admin-facing "Attendance History" page
      (requires ATTENDANCE_READ) - most self-service employees only have ATTENDANCE_SELF_MARK,
      so without this section here they'd have no way to see their own past attendance at all.
      Not permission-gated itself - ATTENDANCE_SELF_MARK (already required for this whole page)
      is enough, since myHistory() on the backend is hard-scoped to the caller's own records. */
  readonly hasSeparateHistoryPage = computed(() => this.authState.hasPermission('ATTENDANCE_READ'));

  readonly showHistory = signal(false);
  readonly loadingHistory = signal(false);
  readonly historyLoaded = signal(false);
  readonly history = signal<AttendanceResponse[]>([]);

  /** Company Feature Configuration override (see FeatureStateService) - if the Super Admin has
      switched Employee Self Attendance off for this company, Check-In/Check-Out never render at
      all here, matching the spec's "Employees must not see Check In/Check Out buttons" exactly.
      The backend independently rejects the API calls too - this is purely so a disabled feature
      never even LOOKS available in the first place. */
  readonly selfAttendanceEnabled = computed(() =>
    this.featureState.isEnabled('ATTENDANCE_MANAGEMENT') && this.featureState.isEnabled('EMPLOYEE_SELF_ATTENDANCE'));
  private tickHandle: ReturnType<typeof setInterval> | null = null;

  readonly loading = signal(true);
  readonly working = signal(false);
  readonly todayStatus = signal<AttendanceResponse | null>(null);
  readonly locationError = signal<string | null>(null);
  readonly selectedWorkMode = signal<WorkMode>('OFFICE');
  readonly workModes = WORK_MODES;
  /** Ticks once a minute while checked in but not yet out, purely to force the live duration label to re-render. */
  readonly nowTick = signal(Date.now());

  readonly isCheckedIn = computed(() => {
    const s = this.todayStatus();
    return !!s && !!s.checkInTime && !s.checkOutTime;
  });
  readonly isCompleted = computed(() => {
    const s = this.todayStatus();
    return !!s && !!s.checkOutTime;
  });
  readonly isLegacyMarked = computed(() => {
    const s = this.todayStatus();
    return !!s && !s.checkInTime;
  });

  readonly liveWorkingDuration = computed(() => {
    const s = this.todayStatus();
    this.nowTick(); // dependency, so this recomputes every tick
    if (!s?.checkInTime) return '';
    const start = new Date(s.checkInTime).getTime();
    const minutes = Math.max(0, Math.floor((Date.now() - start) / 60000));
    return this.formatMinutes(minutes);
  });

  constructor() {
    this.load();
    this.tickHandle = setInterval(() => this.nowTick.set(Date.now()), 60_000);
  }

  ngOnDestroy(): void {
    if (this.tickHandle) clearInterval(this.tickHandle);
  }

  private load(): void {
    this.loading.set(true);
    this.attendanceService.today().subscribe({
      next: status => { this.todayStatus.set(status); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  formatMinutes(totalMinutes?: number): string {
    if (totalMinutes == null) return '-';
    const h = Math.floor(totalMinutes / 60);
    const m = totalMinutes % 60;
    return `${h}h ${m}m`;
  }

  checkIn(): void {
    this.working.set(true);
    this.locationError.set(null);
    this.withLocation((lat, lng) => {
      this.attendanceService.checkIn({ workMode: this.selectedWorkMode(), latitude: lat, longitude: lng }).subscribe({
        next: response => {
          this.working.set(false);
          this.todayStatus.set(response);
          this.toast.success('Checked in - have a great day!');
        },
        error: err => {
          this.working.set(false);
          this.toast.error(err.error?.message ?? 'Unable to check in.');
        }
      });
    });
  }

  checkOut(): void {
    this.working.set(true);
    this.locationError.set(null);
    this.withLocation((lat, lng) => {
      this.attendanceService.checkOut({ latitude: lat, longitude: lng }).subscribe({
        next: response => {
          this.working.set(false);
          this.todayStatus.set(response);
          this.toast.success('Checked out - see you next time!');
        },
        error: err => {
          this.working.set(false);
          this.toast.error(err.error?.message ?? 'Unable to check out.');
        }
      });
    });
  }

  /** Kept for anyone who wants the old no-time-tracking one-click mark instead. */
  markMineOldFlow(): void {
    this.working.set(true);
    this.locationError.set(null);
    this.withLocation((lat, lng) => {
      this.attendanceService.markMine(lat, lng).subscribe({
        next: response => {
          this.working.set(false);
          this.todayStatus.set(response);
          this.toast.success('Your attendance has been marked for today.');
        },
        error: err => {
          this.working.set(false);
          this.toast.error(err.error?.message ?? 'Unable to mark your attendance.');
        }
      });
    });
  }

  private withLocation(action: (lat?: number, lng?: number) => void): void {
    if (!navigator.geolocation) {
      action();
      return;
    }
    navigator.geolocation.getCurrentPosition(
      position => action(position.coords.latitude, position.coords.longitude),
      () => {
        this.locationError.set('Could not access your location - trying anyway (this only matters if your site requires it).');
        action();
      },
      { enableHighAccuracy: true, timeout: 8000 }
    );
  }

  /** Loads on first expand only - a self-service employee opening this once per visit doesn't need it re-fetched every render. */
  toggleHistory(): void {
    const opening = !this.showHistory();
    this.showHistory.set(opening);
    if (opening && !this.historyLoaded()) {
      this.loadHistory();
    }
  }

  private loadHistory(): void {
    this.loadingHistory.set(true);
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 30);
    const iso = (d: Date) => d.toISOString().slice(0, 10);
    this.attendanceService.myHistory(iso(from), iso(to)).subscribe({
      next: records => {
        this.history.set(records);
        this.historyLoaded.set(true);
        this.loadingHistory.set(false);
      },
      error: () => this.loadingHistory.set(false)
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
