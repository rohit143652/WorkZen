import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, inject, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AttendanceService } from '../../services/attendance.service';
import { MonthlyAttendanceReportResponse, MonthlyAttendanceReportRow } from '../../models/attendance.model';
import { SiteService } from '../../../site_module/services/site.service';
import { SiteResponse } from '../../../site_module/models/site.model';
import { ToastService } from '../../../shared/services/toast.service';
import { PaidLeaveService } from '../../../leave_module/services/paid-leave.service';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

/**
 * Bulk, all-employees-at-once ATTENDANCE report - CLIENT_ADMIN only (see
 * MONTHLY_PAYMENT_REPORT_EXPORT permission; route guard + *appHasPermission
 * enforce this in the shell, the backend enforces it independently).
 * Select month/year -> preview the calculated table in-page -> download as
 * Excel or PDF once satisfied.
 *
 * Architecture refactor Phase 4: this report contains NO money - Gross,
 * PF, ESI, Professional Tax, Advance Recovery, and Net Pay all live on the
 * Payroll Processing screen instead (see /payroll/runs), sourced from a
 * persisted PayrollRun's PayrollRunEmployee results, never from live
 * attendance calculation. Viewing this page never writes anything except
 * the one deliberate, explicit "correct one employee's paid leave for
 * this month" action below (see startEditPaidLeave) - a Leave-domain
 * correction, not a passive side effect of viewing.
 *
 * Paid leave policy (default monthly allocation, carry-forward rules, and
 * per-employee extra grants) is configured in the Paid Leave module - see
 * /paid-leave/settings and each Employee's Leave section.
 */
@Component({
  selector: 'app-monthly-report',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './monthly-report.component.html'
})
export class MonthlyReportComponent {
  private readonly attendanceService = inject(AttendanceService);
  private readonly siteService = inject(SiteService);
  private readonly toast = inject(ToastService);
  private readonly paidLeaveService = inject(PaidLeaveService);

  /** Whether Paid Leave is currently switched on for this client - when off, the Paid/Unpaid Leave and Leave Balance columns are hidden entirely rather than showing meaningless zeros. */
  readonly paidLeaveEnabled = signal(true);

  readonly months = MONTH_NAMES.map((label, index) => ({ label, value: index + 1 }));
  readonly years: number[] = (() => {
    const current = new Date().getFullYear();
    return [current, current - 1, current - 2];
  })();

  year = new Date().getFullYear();
  month = new Date().getMonth() + 1;

  // ---- Site filter dropdown: default is "All Sites" (no filter); admin can narrow to one or more ----
  readonly sites = signal<SiteResponse[]>([]);
  readonly allSites = signal(true);
  readonly selectedSiteIds = signal<Set<number>>(new Set());
  readonly siteDropdownOpen = signal(false);

  constructor() {
    this.siteService.list(0, 200).subscribe({
      next: page => this.sites.set(page.content),
      error: () => this.toast.error('Unable to load sites for filtering.')
    });
  }

  /**
   * Scoped to just the site filter dropdown, NOT the whole component - elementRef.nativeElement
   * used to be the entire page's root, so "click anywhere outside the dropdown" was accidentally
   * checking "click anywhere outside the whole page", which is never true while you're still on
   * it. This ViewChild is the small wrapper div around just the button+panel instead.
   */
  @ViewChild('siteFilterWrapper') private siteFilterWrapper?: ElementRef<HTMLElement>;

  /** Closes the site dropdown when clicking anywhere outside the dropdown itself. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.siteDropdownOpen() && this.siteFilterWrapper && !this.siteFilterWrapper.nativeElement.contains(event.target as Node)) {
      this.siteDropdownOpen.set(false);
    }
  }

  toggleSiteDropdown(): void {
    this.siteDropdownOpen.set(!this.siteDropdownOpen());
  }

  siteFilterLabel(): string {
    if (this.allSites()) return 'All Sites';
    const selectedNames = this.sites()
      .filter(s => this.selectedSiteIds().has(s.id))
      .map(s => s.siteName);
    if (selectedNames.length === 0) return 'Select sites...';
    if (selectedNames.length <= 3) return selectedNames.join(', ');
    return `${selectedNames.slice(0, 2).join(', ')} +${selectedNames.length - 2} more`;
  }

  toggleAllSites(): void {
    this.allSites.set(true);
    this.selectedSiteIds.set(new Set());
  }

  toggleSite(siteId: number): void {
    const next = new Set(this.selectedSiteIds());
    if (next.has(siteId)) { next.delete(siteId); } else { next.add(siteId); }
    this.selectedSiteIds.set(next);
    this.allSites.set(next.size === 0);
  }

  isSiteSelected(siteId: number): boolean {
    return this.selectedSiteIds().has(siteId);
  }

  private currentSiteIdsParam(): number[] | undefined {
    return this.allSites() ? undefined : Array.from(this.selectedSiteIds());
  }

  readonly loading = signal(false);
  readonly downloading = signal<'xlsx' | 'pdf' | null>(null);
  readonly report = signal<MonthlyAttendanceReportResponse | null>(null);

  // ---- Inline "edit paid leave" directly in the report table ----
  readonly editingRowCode = signal<string | null>(null);
  readonly savingRowCode = signal<string | null>(null);
  editPaidLeaveValue = 0;

  generatePreview(): void {
    this.loading.set(true);
    this.attendanceService.monthlyReportPreview(this.year, this.month, this.currentSiteIdsParam()).subscribe({
      next: report => { this.report.set(report); this.loading.set(false); },
      error: () => {
        this.loading.set(false);
        this.toast.error('Unable to generate the report. Please try again.');
      }
    });
    // Whether Paid Leave was active is specific to the exact month being previewed - a policy
    // scheduled for a future date within this same month (e.g. switches off partway through)
    // still resolves using that month's OWN first day, matching the backend's Excel/PDF logic
    // exactly - never "today's" policy, which could disagree with the month actually on screen.
    this.paidLeaveService.getConfigForMonth(this.year, this.month).subscribe({
      next: config => this.paidLeaveEnabled.set(config.enabled),
      error: () => { /* if this fails, default to showing the columns rather than hiding real data */ }
    });
  }

  download(format: 'xlsx' | 'pdf'): void {
    this.downloading.set(format);
    this.attendanceService.downloadMonthlyReport(this.year, this.month, format, this.currentSiteIdsParam()).subscribe({
      next: blob => {
        const monthLabel = String(this.month).padStart(2, '0');
        const filename = `Monthly-Attendance-Report-${this.year}-${monthLabel}.${format}`;
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = filename;
        anchor.click();
        window.URL.revokeObjectURL(url);
        this.downloading.set(null);
        this.toast.success('Report downloaded.');
      },
      error: () => {
        this.downloading.set(null);
        this.toast.error('Unable to generate the report. Please try again.');
      }
    });
  }

  startEditPaidLeave(row: MonthlyAttendanceReportRow): void {
    if (row.onLeaveDays === 0) {
      this.toast.warning(`${row.employeeName} has no On Leave days marked for this month, so there's nothing to adjust.`);
      return;
    }
    this.editingRowCode.set(row.employeeCode);
    this.editPaidLeaveValue = row.paidLeaveDays;
  }

  cancelEditPaidLeave(): void {
    this.editingRowCode.set(null);
  }

  saveEditPaidLeave(row: MonthlyAttendanceReportRow): void {
    const maxAllowed = row.onLeaveDays;
    if (this.editPaidLeaveValue < 0 || this.editPaidLeaveValue > maxAllowed) {
      this.toast.warning(
        maxAllowed === 0
          ? `${row.employeeName} has no On Leave days marked for this month, so there's nothing to mark as paid.`
          : `Paid leave for ${row.employeeName} must be between 0 and ${maxAllowed} (the On Leave days marked this month).`
      );
      return;
    }
    this.savingRowCode.set(row.employeeCode);
    this.attendanceService.adjustPaidLeave(row.employeeId, this.year, this.month, this.editPaidLeaveValue).subscribe({
      next: () => {
        this.savingRowCode.set(null);
        this.editingRowCode.set(null);
        this.toast.success(`Paid leave updated for ${row.employeeName}.`);
        this.generatePreview();
      },
      error: err => {
        this.savingRowCode.set(null);
        this.toast.error(err.error?.message ?? 'Unable to update paid leave.');
      }
    });
  }

  resetRowToAuto(row: MonthlyAttendanceReportRow): void {
    this.savingRowCode.set(row.employeeCode);
    this.attendanceService.adjustPaidLeave(row.employeeId, this.year, this.month, null).subscribe({
      next: () => {
        this.savingRowCode.set(null);
        this.editingRowCode.set(null);
        this.toast.success(`${row.employeeName} reverted to the auto-calculated figure.`);
        this.generatePreview();
      },
      error: err => {
        this.savingRowCode.set(null);
        this.toast.error(err.error?.message ?? 'Unable to reset this adjustment.');
      }
    });
  }
}
