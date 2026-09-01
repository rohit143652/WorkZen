import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AttendanceService } from '../../services/attendance.service';
import { AttendanceResponse } from '../../models/attendance.model';
import { ToastService } from '../../../shared/services/toast.service';

/**
 * "Mark My Attendance" - a one-click self-check-in for anyone with a login, distinct from the
 * supervisor-driven Mark Attendance page (which marks OTHER people's attendance from a table).
 * Always marks today, always PRESENT - there's nothing else to fill in, which is the whole
 * point of "one click". If the employee's assigned site has a GPS geofence configured, the
 * backend rejects the mark with a clear distance-based message when they're too far away.
 */
@Component({
  selector: 'app-mark-my-attendance',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mark-my-attendance.component.html'
})
export class MarkMyAttendanceComponent {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(true);
  readonly marking = signal(false);
  readonly todayStatus = signal<AttendanceResponse | null>(null);
  readonly locationError = signal<string | null>(null);

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.attendanceService.myTodayStatus().subscribe({
      next: status => { this.todayStatus.set(status); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  markMyAttendance(): void {
    this.marking.set(true);
    this.locationError.set(null);

    if (!navigator.geolocation) {
      this.submitMark();
      return;
    }
    navigator.geolocation.getCurrentPosition(
      position => this.submitMark(position.coords.latitude, position.coords.longitude),
      () => {
        this.locationError.set('Could not access your location - trying anyway (this only matters if your site requires it).');
        this.submitMark();
      },
      { enableHighAccuracy: true, timeout: 8000 }
    );
  }

  private submitMark(latitude?: number, longitude?: number): void {
    this.attendanceService.markMine(latitude, longitude).subscribe({
      next: response => {
        this.marking.set(false);
        this.todayStatus.set(response);
        this.toast.success('Your attendance has been marked for today.');
      },
      error: err => {
        this.marking.set(false);
        this.toast.error(err.error?.message ?? 'Unable to mark your attendance.');
      }
    });
  }
}
