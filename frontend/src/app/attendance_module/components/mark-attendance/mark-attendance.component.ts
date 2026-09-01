import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AttendanceService } from '../../services/attendance.service';
import { ATTENDANCE_STATUSES, AttendanceStatus, EmployeeAttendanceOption } from '../../models/attendance.model';
import { SiteService } from '../../../site_module/services/site.service';
import { SiteResponse } from '../../../site_module/models/site.model';
import { StatusBadgeComponent, BadgeKind } from '../../../shared/components/status-badge/status-badge.component';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

@Component({
  selector: 'app-mark-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './mark-attendance.component.html'
})
export class MarkAttendanceComponent {
  private readonly attendanceService = inject(AttendanceService);
  private readonly siteService = inject(SiteService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly today = todayIso();
  selectedDate = this.today;
  selectedSiteId: number | null = null;
  /** Client-side filter over the already-loaded rows for this date/site - lets an admin quickly jump to one employee in a long list rather than scrolling to find them. */
  searchTerm = '';

  readonly sites = signal<SiteResponse[]>([]);
  readonly rows = signal<EmployeeAttendanceOption[]>([]);
  readonly loading = signal(true);
  readonly savingAll = signal(false);
  readonly statuses = ATTENDANCE_STATUSES;

  /** Per-row selection while marking - keyed by employeeId, reset whenever the date/site filter changes. */
  selections = new Map<number, AttendanceStatus>();
  remarksByEmployee = new Map<number, string>();

  constructor() {
    this.siteService.list().subscribe(res => this.sites.set(res.content));
    this.load();
  }

  onFilterChange(): void {
    this.load();
  }

  filteredRows(): EmployeeAttendanceOption[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) return this.rows();
    return this.rows().filter(r =>
      r.employeeName.toLowerCase().includes(term) || r.employeeCode.toLowerCase().includes(term));
  }

  private load(): void {
    this.loading.set(true);
    this.selections.clear();
    this.remarksByEmployee.clear();
    this.attendanceService.markable(this.selectedDate, this.selectedSiteId).subscribe({
      next: rows => { this.rows.set(rows); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Unable to load employees.'); }
    });
  }

  setStatus(employeeId: number, status: AttendanceStatus | ''): void {
    if (status === '') {
      this.selections.delete(employeeId);
    } else {
      this.selections.set(employeeId, status);
    }
  }

  /** Unmarked rows still without an explicit selection - the count driving the "Save All" button label. */
  get pendingCount(): number {
    return this.rows().filter(r => !r.existingRecord && this.selections.has(r.employeeId)).length;
  }

  get unmarkedCount(): number {
    return this.rows().filter(r => !r.existingRecord).length;
  }

  badgeKind(status: AttendanceStatus): BadgeKind {
    switch (status) {
      case 'PRESENT': return 'success';
      case 'ABSENT': return 'danger';
      case 'HALF_DAY': return 'warning';
      case 'ON_LEAVE': return 'info';
    }
  }

  statusLabel(status: AttendanceStatus): string {
    return this.statuses.find(s => s.value === status)?.label ?? status;
  }

  /**
   * The common case (most employees present most days) shouldn't need N
   * individual clicks either - this fills every still-unmarked row with
   * PRESENT in one action; anyone who actually needs a different status
   * can still change their own dropdown afterward before Save All.
   */
  markAllPresent(): void {
    for (const row of this.rows()) {
      if (!row.existingRecord) {
        this.selections.set(row.employeeId, 'PRESENT');
      }
    }
  }

  clearSelections(): void {
    this.selections.clear();
  }

  /** Best-effort GPS capture - resolves to {} (no coordinates) rather than rejecting if the
      browser has no geolocation support, the user denies permission, or it times out, since most
      sites have no geofence configured at all and shouldn't be blocked from marking attendance
      over this. Sites that DO have a geofence will get a clear rejection from the backend
      instead, naming exactly which employees couldn't be marked and why. */
  private getCurrentLocation(): Promise<{ latitude?: number; longitude?: number }> {
    return new Promise(resolve => {
      if (!navigator.geolocation) { resolve({}); return; }
      navigator.geolocation.getCurrentPosition(
        position => resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude }),
        () => resolve({}),
        { enableHighAccuracy: true, timeout: 8000 }
      );
    });
  }

  /**
   * One request for every selected row instead of one request PER row -
   * this is the actual fix for "100 employees would mean 100 saves".
   */
  async saveAll(): Promise<void> {
    const entries = this.rows()
      .filter(row => !row.existingRecord && this.selections.has(row.employeeId))
      .map(row => ({
        employeeId: row.employeeId,
        status: this.selections.get(row.employeeId)!,
        remarks: this.remarksByEmployee.get(row.employeeId) || undefined
      }));

    if (entries.length === 0) {
      this.toast.warning('Select a status for at least one employee first.');
      return;
    }

    const ok = await this.confirmDialog.ask({
      title: 'Save attendance?',
      message: `Save attendance for ${entries.length} employee(s) on ${this.selectedDate}? `
        + `Once saved, these cannot be changed by you - only a Client Admin can correct them afterward.`,
      confirmLabel: `Save ${entries.length} Record(s)`
    });
    if (!ok) return;

    // Best-effort - a site with no GPS geofence configured doesn't need this at all, and even a
    // site that DOES have one will get a clear per-employee rejection from the backend (not a
    // silent failure) if location access isn't available, so there's nothing to block on here.
    const location = await this.getCurrentLocation();

    this.savingAll.set(true);
    this.attendanceService.bulkMark({ attendanceDate: this.selectedDate, entries, ...location }).subscribe({
      next: result => {
        this.savingAll.set(false);
        if (result.rejected.length > 0) {
          this.toast.warning(`${result.marked} of ${result.requested} saved. ${result.rejected.length} could not be saved - see details.`);
        } else {
          this.toast.success(`${result.marked} attendance record(s) saved successfully.`);
        }
        this.load();
      },
      error: err => {
        this.savingAll.set(false);
        this.toast.error(err.error?.message ?? 'Unable to save attendance.');
      }
    });
  }
}
