import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HolidayService } from '../../services/holiday.service';
import { HolidayResponse } from '../../models/holiday.model';
import { FestivalRef, getFestivalRef } from '../../models/indian-festivals';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';

interface CalendarCell {
  date: Date;
  dateStr: string;
  dayNum: number;
  inCurrentMonth: boolean;
  isToday: boolean;
  isPast: boolean;
  holiday?: HolidayResponse;
  festival?: FestivalRef;
}

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];
const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

/** Local-date formatting (not toISOString(), which converts to UTC and can shift the calendar day near midnight). */
function toDateStr(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

@Component({
  selector: 'app-holiday-calendar',
  standalone: true,
  imports: [CommonModule, FormsModule, HasPermissionDirective],
  templateUrl: './holiday-calendar.component.html'
})
export class HolidayCalendarComponent {
  private readonly holidayService = inject(HolidayService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly today = new Date();

  readonly monthNames = MONTH_NAMES;
  readonly weekdayLabels = WEEKDAY_LABELS;

  // ---- Month/Year quick-jump picker (opened by clicking the "Month Year" title) ----
  readonly showMonthYearPicker = signal(false);
  readonly pickerYears = computed(() => {
    const current = this.today.getFullYear();
    const years: number[] = [];
    for (let y = current - 5; y <= current + 5; y++) years.push(y);
    return years;
  });
  pickerMonth = this.today.getMonth();
  pickerYear = this.today.getFullYear();

  openMonthYearPicker(): void {
    this.pickerMonth = this.viewMonth();
    this.pickerYear = this.viewYear();
    this.showMonthYearPicker.set(true);
  }

  applyMonthYearPicker(): void {
    this.viewMonth.set(this.pickerMonth);
    this.viewYear.set(this.pickerYear);
    this.showMonthYearPicker.set(false);
  }

  closeMonthYearPicker(): void {
    this.showMonthYearPicker.set(false);
  }

  readonly holidays = signal<HolidayResponse[]>([]);
  readonly loading = signal(true);

  readonly viewYear = signal(this.today.getFullYear());
  readonly viewMonth = signal(this.today.getMonth()); // 0-indexed, matches JS Date

  readonly calendarCells = computed<CalendarCell[]>(() => {
    const year = this.viewYear();
    const month = this.viewMonth();
    const todayStr = toDateStr(this.today);

    const firstOfMonth = new Date(year, month, 1);
    const startWeekday = firstOfMonth.getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const cells: CalendarCell[] = [];
    // Leading days from the previous month, so the grid always starts on Sunday.
    for (let i = startWeekday - 1; i >= 0; i--) {
      const d = new Date(year, month, -i);
      cells.push(this.buildCell(d, false, todayStr));
    }
    // The month itself.
    for (let day = 1; day <= daysInMonth; day++) {
      const d = new Date(year, month, day);
      cells.push(this.buildCell(d, true, todayStr));
    }
    // Trailing days from the next month, padding out to full weeks (42 cells = 6 rows, always).
    while (cells.length < 42) {
      const lastDate = cells[cells.length - 1].date;
      const d = new Date(lastDate);
      d.setDate(d.getDate() + 1);
      cells.push(this.buildCell(d, false, todayStr));
    }
    return cells;
  });

  private buildCell(date: Date, inCurrentMonth: boolean, todayStr: string): CalendarCell {
    const dateStr = toDateStr(date);
    return {
      date,
      dateStr,
      dayNum: date.getDate(),
      inCurrentMonth,
      isToday: dateStr === todayStr,
      isPast: dateStr < todayStr,
      holiday: this.holidays().find(h => dateStr >= h.startDate && dateStr <= h.endDate),
      festival: getFestivalRef(dateStr)
    };
  }

  // ---- Add/View modal ----
  readonly showAddModal = signal(false);
  readonly showViewModal = signal(false);
  readonly saving = signal(false);
  readonly deleting = signal(false);
  viewingHoliday: HolidayResponse | null = null;
  formStartDate = '';
  formEndDate = '';
  formName = '';
  formDescription = '';

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.holidayService.list().subscribe({
      next: list => { this.holidays.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  prevMonth(): void {
    const m = this.viewMonth();
    if (m === 0) { this.viewMonth.set(11); this.viewYear.set(this.viewYear() - 1); }
    else { this.viewMonth.set(m - 1); }
  }

  nextMonth(): void {
    const m = this.viewMonth();
    if (m === 11) { this.viewMonth.set(0); this.viewYear.set(this.viewYear() + 1); }
    else { this.viewMonth.set(m + 1); }
  }

  goToToday(): void {
    this.viewYear.set(this.today.getFullYear());
    this.viewMonth.set(this.today.getMonth());
  }

  onDayClick(cell: CalendarCell): void {
    if (cell.holiday) {
      this.viewingHoliday = cell.holiday;
      this.showViewModal.set(true);
      return;
    }
    if (cell.isPast) {
      this.toast.warning('A new holiday can only be added from today onward - past dates can be viewed but not added.');
      return;
    }
    // Both start and end default to the clicked date - the admin only needs to change the end
    // date if this holiday actually spans more than one day.
    this.formStartDate = cell.dateStr;
    this.formEndDate = cell.dateStr;
    this.formName = cell.festival?.name ?? '';
    this.formDescription = '';
    this.showAddModal.set(true);
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
  }

  closeViewModal(): void {
    this.showViewModal.set(false);
    this.viewingHoliday = null;
  }

  async submitNewHoliday(): Promise<void> {
    if (!this.formStartDate || !this.formEndDate || !this.formName.trim()) return;
    if (this.formEndDate < this.formStartDate) {
      this.toast.error('End date cannot be before start date.');
      return;
    }

    const ok = await this.confirmDialog.ask({
      title: 'Add this holiday?',
      message: `This will mark every currently active employee Present from ${this.formStartDate} to ${this.formEndDate}, `
        + `which counts toward Payable Days for that month's payroll. This cannot be undone by deleting the holiday afterward - `
        + `the attendance records it creates stay in place.`,
      confirmLabel: 'Add Holiday & Mark Present'
    });
    if (!ok) return;

    this.saving.set(true);
    this.holidayService.create({
      startDate: this.formStartDate,
      endDate: this.formEndDate,
      name: this.formName.trim(),
      description: this.formDescription.trim() || undefined
    }).subscribe({
      next: created => {
        this.saving.set(false);
        this.showAddModal.set(false);
        this.toast.success(`"${created.name}" added - ${created.employeesMarkedPresent} attendance record(s) marked Present.`);
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(this.extractErrorMessage(err));
      }
    });
  }

  /** Surfaces the real field-level reason when the backend's generic "message" is just "Validation failed" - see GlobalExceptionHandler.handleValidation(), which puts the actual per-field reasons in a separate "errors" map that a plain err.error?.message read would otherwise miss entirely. */
  private extractErrorMessage(err: any): string {
    const fieldErrors = err.error?.errors;
    if (fieldErrors && typeof fieldErrors === 'object') {
      const details = Object.entries(fieldErrors).map(([field, msg]) => `${field}: ${msg}`).join('; ');
      if (details) return details;
    }
    return err.error?.message ?? 'Unable to add this holiday.';
  }

  async deleteHoliday(holiday: HolidayResponse): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Remove this holiday?',
      message: `Remove "${holiday.name}" (${holiday.startDate} to ${holiday.endDate}) from the calendar? `
        + `The attendance records it already created will NOT be un-marked - only future payroll runs are affected.`,
      confirmLabel: 'Remove Holiday'
    });
    if (!ok) return;

    this.deleting.set(true);
    this.holidayService.delete(holiday.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.closeViewModal();
        this.toast.success(`"${holiday.name}" removed from the calendar.`);
        this.load();
      },
      error: err => {
        this.deleting.set(false);
        this.toast.error(err.error?.message ?? 'Unable to remove this holiday.');
      }
    });
  }
}
