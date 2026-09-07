import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { SiteService } from '../../../site_module/services/site.service';
import { SiteResponse } from '../../../site_module/models/site.model';
import { StatusBadgeComponent, BadgeKind } from '../../../shared/components/status-badge/status-badge.component';
import { AttendanceService } from '../../../attendance_module/services/attendance.service';
import { AttendanceResponse, TodayAttendanceOverviewResponse } from '../../../attendance_module/models/attendance.model';
import { CalendarService } from '../../../calendar_module/services/calendar.service';
import { CalendarItemResponse } from '../../../calendar_module/models/calendar.model';
import { ToastService } from '../../../shared/services/toast.service';
import { FeatureStateService } from '../../../core/services/feature-state.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  private readonly siteService = inject(SiteService);
  private readonly attendanceService = inject(AttendanceService);
  private readonly calendarService = inject(CalendarService);
  private readonly toast = inject(ToastService);
  private readonly featureState = inject(FeatureStateService);
  readonly authState = inject(AuthStateService);

  readonly sites = signal<SiteResponse[]>([]);
  readonly loadingSites = signal(false);

  readonly hasSiteReadPermission = this.authState.hasPermission('SITE_READ');
  readonly hasSelfMarkPermission = this.authState.hasPermission('ATTENDANCE_SELF_MARK');
  /** Company Feature Configuration is the upper-level control (see FeatureAccessService on the
      backend) - the Check-In widget only shows when BOTH the role permission allows it AND the
      company hasn't switched Attendance Management or Employee Self Attendance off. */
  readonly canSelfMark = computed(() =>
    this.hasSelfMarkPermission
    && this.featureState.isEnabled('ATTENDANCE_MANAGEMENT')
    && this.featureState.isEnabled('EMPLOYEE_SELF_ATTENDANCE'));
  readonly hasAttendanceReadPermission = this.authState.hasPermission('ATTENDANCE_READ');
  /** Same upper-level rule applied to the admin "Today's Attendance Overview" widget - if the
      whole module is off for this company, this has nothing meaningful to show either. */
  readonly canSeeAttendanceOverview = computed(() =>
    this.hasAttendanceReadPermission && this.featureState.isEnabled('ATTENDANCE_MANAGEMENT'));
  readonly hasEventReadPermission = this.authState.hasPermission('EVENT_READ');

  /** True when none of the widgets below apply to this account at all - shown a friendly
      "nothing to see yet" state instead of a page that's just a header and empty space. */
  readonly hasNoWidgets = computed(() =>
    !this.canSelfMark() && !this.hasEventReadPermission
    && !this.canSeeAttendanceOverview() && !this.hasSiteReadPermission);

  // ---- Check-in / Check-out (everyone with ATTENDANCE_SELF_MARK) ----
  readonly loadingToday = signal(true);
  readonly todayStatus = signal<AttendanceResponse | null>(null);
  readonly checkingInOut = signal(false);

  // ---- Present / Not-checked-in overview (admins with ATTENDANCE_READ) ----
  readonly loadingOverview = signal(true);
  readonly overview = signal<TodayAttendanceOverviewResponse | null>(null);

  // ---- Today's meetings (everyone with EVENT_READ) ----
  readonly loadingMeetings = signal(true);
  readonly todaysMeetings = signal<CalendarItemResponse[]>([]);

  constructor() {
    if (this.hasSiteReadPermission) {
      this.loadSites();
    }
    if (this.canSelfMark()) {
      this.loadToday();
    } else {
      this.loadingToday.set(false);
    }
    if (this.canSeeAttendanceOverview()) {
      this.loadOverview();
    } else {
      this.loadingOverview.set(false);
    }
    if (this.hasEventReadPermission) {
      this.loadTodaysMeetings();
    } else {
      this.loadingMeetings.set(false);
    }
  }

  private loadToday(): void {
    this.loadingToday.set(true);
    this.attendanceService.today().subscribe({
      next: status => { this.todayStatus.set(status); this.loadingToday.set(false); },
      error: () => this.loadingToday.set(false)
    });
  }

  quickCheckIn(): void {
    this.checkingInOut.set(true);
    const submit = (lat?: number, lng?: number) => {
      this.attendanceService.checkIn({ workMode: 'OFFICE', latitude: lat, longitude: lng }).subscribe({
        next: response => {
          this.checkingInOut.set(false);
          this.todayStatus.set(response);
          this.toast.success('Checked in!');
          this.refreshOverviewIfVisible();
        },
        error: err => { this.checkingInOut.set(false); this.toast.error(err.error?.message ?? 'Unable to check in.'); }
      });
    };
    if (!navigator.geolocation) { submit(); return; }
    navigator.geolocation.getCurrentPosition(pos => submit(pos.coords.latitude, pos.coords.longitude), () => submit(), { timeout: 8000 });
  }

  quickCheckOut(): void {
    this.checkingInOut.set(true);
    const submit = (lat?: number, lng?: number) => {
      this.attendanceService.checkOut({ latitude: lat, longitude: lng }).subscribe({
        next: response => {
          this.checkingInOut.set(false);
          this.todayStatus.set(response);
          this.toast.success('Checked out!');
          this.refreshOverviewIfVisible();
        },
        error: err => { this.checkingInOut.set(false); this.toast.error(err.error?.message ?? 'Unable to check out.'); }
      });
    };
    if (!navigator.geolocation) { submit(); return; }
    navigator.geolocation.getCurrentPosition(pos => submit(pos.coords.latitude, pos.coords.longitude), () => submit(), { timeout: 8000 });
  }

  private loadOverview(): void {
    this.loadingOverview.set(true);
    this.attendanceService.todayOverview().subscribe({
      next: o => { this.overview.set(o); this.loadingOverview.set(false); },
      error: () => this.loadingOverview.set(false)
    });
  }

  /** Called after this same page's own quick check-in/check-out succeeds - without this, the
      "Today's Attendance Overview" list below (visible to anyone with ATTENDANCE_READ, which
      commonly includes the same admin account doing the checking-in/out) would keep showing
      whatever it loaded on page load until a manual refresh, e.g. still "Working" for someone
      who just checked out a second ago. Silently does nothing if this account can't see that
      widget at all - not every account that can check in can also see the admin overview. */
  private refreshOverviewIfVisible(): void {
    if (this.canSeeAttendanceOverview()) {
      this.loadOverview();
    }
  }

  private loadTodaysMeetings(): void {
    this.loadingMeetings.set(true);
    const now = new Date();
    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    const endOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, 0);
    this.calendarService.findInRange(this.toLocalIso(startOfDay), this.toLocalIso(endOfDay)).subscribe({
      next: items => {
        this.todaysMeetings.set(items.filter(i => i.type === 'EVENT').sort((a, b) => a.startAt.localeCompare(b.startAt)));
        this.loadingMeetings.set(false);
      },
      error: () => this.loadingMeetings.set(false)
    });
  }

  private toLocalIso(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  private loadSites(): void {
    this.loadingSites.set(true);
    this.siteService.list(0, 100).subscribe({
      next: res => { this.sites.set(res.content); this.loadingSites.set(false); },
      error: () => this.loadingSites.set(false)
    });
  }

  allocationStatus(site: SiteResponse): { label: string; kind: BadgeKind } {
    if (site.requiredEmployeeCount <= 0) return { label: 'No target set', kind: 'muted' };
    if (site.assignedEmployeeCount > site.requiredEmployeeCount) return { label: 'Overallocated', kind: 'warning' };
    if (site.assignedEmployeeCount === site.requiredEmployeeCount) return { label: 'Full', kind: 'success' };
    return { label: 'Understaffed', kind: 'info' };
  }
}
