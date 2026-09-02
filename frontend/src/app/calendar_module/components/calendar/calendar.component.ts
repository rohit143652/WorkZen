import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CalendarService, EventService } from '../../services/calendar.service';
import { CalendarItemResponse, CalendarViewMode } from '../../models/calendar.model';
import { HolidayService } from '../../../holiday_module/services/holiday.service';
import { getFestivalRef, getFestivalsForYear } from '../../../holiday_module/models/indian-festivals';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';
import { EventFormModalComponent } from '../event-form-modal/event-form-modal.component';
import { CalendarItemDetailModalComponent } from '../calendar-item-detail-modal/calendar-item-detail-modal.component';

const HOUR_HEIGHT_PX = 48;
const MONTH_NAMES = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

interface PositionedItem {
  item: CalendarItemResponse;
  topPx: number;
  heightPx: number;
  dayIndex: number; // 0-6 within the visible week/day range
}

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [CommonModule, FormsModule, HasPermissionDirective, EventFormModalComponent, CalendarItemDetailModalComponent],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.css'
})
export class CalendarComponent {
  private readonly calendarService = inject(CalendarService);
  private readonly eventService = inject(EventService);
  private readonly holidayService = inject(HolidayService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  readonly authState = inject(AuthStateService);

  readonly hourHeightPx = HOUR_HEIGHT_PX;
  readonly hours = Array.from({ length: 24 }, (_, i) => i);

  readonly viewMode = signal<CalendarViewMode>('month');
  readonly anchorDate = signal(new Date());
  readonly items = signal<CalendarItemResponse[]>([]);
  readonly loading = signal(true);
  readonly showHolidays = signal(true);
  readonly showEvents = signal(true);
  readonly now = signal(new Date());

  readonly showEventForm = signal(false);
  readonly editingEvent = signal<CalendarItemResponse | null>(null);
  readonly prefillStart = signal<Date | null>(null);
  readonly selectedDetailItem = signal<CalendarItemResponse | null>(null);
  readonly moreItemsForDate = signal<{ date: Date; items: CalendarItemResponse[] } | null>(null);

  // ---- Add Holiday (minimal - reuses the existing HolidayService as-is, see business rules
  // #5/#6/#12: only HOLIDAY_CREATE holders ever see this, backend re-checks regardless) ----
  readonly showHolidayForm = signal(false);
  readonly savingHoliday = signal(false);
  newHolidayName = '';
  newHolidayStartDate = '';
  newHolidayEndDate = '';
  newHolidayDescription = '';

  // ---- Add Year's Holidays (bulk) ----
  readonly showBulkHolidayForm = signal(false);
  readonly savingBulkHolidays = signal(false);
  bulkYear = new Date().getFullYear();
  bulkYearItems: { date: string; name: string; selected: boolean }[] = [];

  constructor() {
    this.load();
    setInterval(() => this.now.set(new Date()), 60000);
  }

  // ---- Range computation ----

  private getRange(): { start: Date; end: Date } {
    const anchor = this.anchorDate();
    if (this.viewMode() === 'day') {
      const start = new Date(anchor.getFullYear(), anchor.getMonth(), anchor.getDate());
      const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 1);
      return { start, end };
    }
    if (this.viewMode() === 'week') {
      const start = new Date(anchor.getFullYear(), anchor.getMonth(), anchor.getDate() - anchor.getDay());
      const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 7);
      return { start, end };
    }
    // month - includes the leading/trailing days of adjoining weeks shown in the grid, so
    // events/holidays on those visible-but-adjacent days still load correctly.
    const firstOfMonth = new Date(anchor.getFullYear(), anchor.getMonth(), 1);
    const start = new Date(firstOfMonth.getFullYear(), firstOfMonth.getMonth(), firstOfMonth.getDate() - firstOfMonth.getDay());
    const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 42);
    return { start, end };
  }

  private toIsoLocal(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
  }

  load(): void {
    this.loading.set(true);
    const { start, end } = this.getRange();
    this.calendarService.findInRange(this.toIsoLocal(start), this.toIsoLocal(end)).subscribe({
      next: items => { this.items.set(items); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  // ---- Navigation ----

  setView(mode: CalendarViewMode): void {
    this.viewMode.set(mode);
    this.load();
  }

  goToday(): void {
    this.anchorDate.set(new Date());
    this.load();
  }

  navigatePrev(): void {
    this.shiftAnchor(-1);
  }

  navigateNext(): void {
    this.shiftAnchor(1);
  }

  private shiftAnchor(direction: 1 | -1): void {
    const a = this.anchorDate();
    if (this.viewMode() === 'day') {
      this.anchorDate.set(new Date(a.getFullYear(), a.getMonth(), a.getDate() + direction));
    } else if (this.viewMode() === 'week') {
      this.anchorDate.set(new Date(a.getFullYear(), a.getMonth(), a.getDate() + 7 * direction));
    } else {
      this.anchorDate.set(new Date(a.getFullYear(), a.getMonth() + direction, 1));
    }
    this.load();
  }

  readonly headerLabel = computed(() => {
    const a = this.anchorDate();
    if (this.viewMode() === 'month') return `${MONTH_NAMES[a.getMonth()]} ${a.getFullYear()}`;
    if (this.viewMode() === 'day') return `${MONTH_NAMES[a.getMonth()].slice(0, 3)} ${a.getDate()}, ${a.getFullYear()}`;
    const { start, end } = this.getRange();
    const endInclusive = new Date(end.getFullYear(), end.getMonth(), end.getDate() - 1);
    const sameMonth = start.getMonth() === endInclusive.getMonth();
    const startLabel = `${MONTH_NAMES[start.getMonth()].slice(0, 3)} ${start.getDate()}`;
    const endLabel = sameMonth ? `${endInclusive.getDate()}` : `${MONTH_NAMES[endInclusive.getMonth()].slice(0, 3)} ${endInclusive.getDate()}`;
    return `${startLabel} - ${endLabel}, ${endInclusive.getFullYear()}`;
  });

  // ---- Filtering ----

  readonly visibleItems = computed(() =>
    this.items().filter(i => (i.type === 'HOLIDAY' ? this.showHolidays() : this.showEvents()))
  );

  toggleShowHolidays(): void { this.showHolidays.set(!this.showHolidays()); }
  toggleShowEvents(): void { this.showEvents.set(!this.showEvents()); }

  // ---- Month view helpers ----

  readonly monthCells = computed(() => {
    const { start } = this.getRange();
    const today = new Date();
    const cells: { date: Date; inCurrentMonth: boolean; isToday: boolean; items: CalendarItemResponse[] }[] = [];
    for (let i = 0; i < 42; i++) {
      const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
      cells.push({
        date: d,
        inCurrentMonth: d.getMonth() === this.anchorDate().getMonth(),
        isToday: this.isSameDate(d, today),
        items: this.visibleItems().filter(it => this.itemOccursOnDate(it, d))
      });
    }
    return cells;
  });

  private isSameDate(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  }

  /** Reference-only Indian festival name for a date (e.g. "Janmashtami") - not an actual company
      Holiday until an authorized user explicitly clicks "Add Holiday" for it (see indian-festivals.ts). */
  festivalNameFor(date: Date): string | undefined {
    const pad = (n: number) => String(n).padStart(2, '0');
    const dateStr = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
    return getFestivalRef(dateStr)?.name;
  }

  private itemOccursOnDate(item: CalendarItemResponse, date: Date): boolean {
    const start = new Date(item.startAt);
    const end = new Date(item.endAt);
    const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const dayEnd = new Date(dayStart.getFullYear(), dayStart.getMonth(), dayStart.getDate() + 1);
    return start < dayEnd && end >= dayStart;
  }

  showMoreForDate(date: Date, items: CalendarItemResponse[]): void {
    this.moreItemsForDate.set({ date, items });
  }

  closeMoreModal(): void {
    this.moreItemsForDate.set(null);
  }

  // ---- Day/Week time-grid helpers ----

  readonly weekDays = computed(() => {
    const { start } = this.getRange();
    const count = this.viewMode() === 'day' ? 1 : 7;
    const today = new Date();
    return Array.from({ length: count }, (_, i) => {
      const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
      return { date: d, isToday: this.isSameDate(d, today) };
    });
  });

  readonly allDayItemsByDay = computed(() =>
    this.weekDays().map(d => this.visibleItems().filter(it => it.allDay && this.itemOccursOnDate(it, d.date)))
  );

  readonly timedPositionedItems = computed<PositionedItem[]>(() => {
    const days = this.weekDays();
    const result: PositionedItem[] = [];
    days.forEach((d, dayIndex) => {
      const dayItems = this.visibleItems().filter(it => !it.allDay && this.itemOccursOnDate(it, d.date));
      for (const item of dayItems) {
        const start = new Date(item.startAt);
        const end = new Date(item.endAt);
        const dayStart = new Date(d.date.getFullYear(), d.date.getMonth(), d.date.getDate());
        const startMinutes = Math.max(0, (start.getTime() - dayStart.getTime()) / 60000);
        const endMinutes = Math.min(24 * 60, (end.getTime() - dayStart.getTime()) / 60000);
        const topPx = (startMinutes / 60) * HOUR_HEIGHT_PX;
        const heightPx = Math.max(18, ((endMinutes - startMinutes) / 60) * HOUR_HEIGHT_PX);
        result.push({ item, topPx, heightPx, dayIndex });
      }
    });
    return result;
  });

  readonly currentTimeTopPx = computed(() => {
    const n = this.now();
    return ((n.getHours() * 60 + n.getMinutes()) / 60) * HOUR_HEIGHT_PX;
  });

  isCurrentTimeVisibleForDay(dayIndex: number): boolean {
    return this.weekDays()[dayIndex]?.isToday ?? false;
  }

  // ---- Create/edit/delete ----

  canCreateEvent(): boolean {
    return this.authState.hasPermission('EVENT_CREATE');
  }

  canCreateHoliday(): boolean {
    return this.authState.hasPermission('HOLIDAY_CREATE');
  }

  openCreateEvent(prefill?: Date): void {
    this.editingEvent.set(null);
    this.prefillStart.set(prefill ?? new Date());
    this.showEventForm.set(true);
  }

  openEditEvent(item: CalendarItemResponse): void {
    this.selectedDetailItem.set(null);
    this.moreItemsForDate.set(null);
    this.editingEvent.set(item);
    this.prefillStart.set(null);
    this.showEventForm.set(true);
  }

  closeEventForm(): void {
    this.showEventForm.set(false);
  }

  onEventSaved(): void {
    this.showEventForm.set(false);
    this.load();
  }

  openDetail(item: CalendarItemResponse): void {
    this.moreItemsForDate.set(null);
    this.selectedDetailItem.set(item);
  }

  closeDetail(): void {
    this.selectedDetailItem.set(null);
  }

  async deleteEvent(item: CalendarItemResponse): Promise<void> {
    if (item.type === 'HOLIDAY') {
      const ok = await this.confirmDialog.ask({
        title: 'Delete this holiday?',
        message: `Delete "${item.title}"? This removes it from every employee's calendar company-wide.`,
        confirmLabel: 'Delete Holiday'
      });
      if (!ok) return;
      this.holidayService.delete(item.id).subscribe({
        next: () => {
          this.toast.success('Holiday deleted.');
          this.selectedDetailItem.set(null);
          this.load();
        },
        error: err => this.toast.error(err.error?.message ?? 'Unable to delete this holiday.')
      });
      return;
    }

    const ok = await this.confirmDialog.ask({
      title: 'Delete this event?',
      message: `Delete "${item.title}"? This cannot be undone.`,
      confirmLabel: 'Delete Event'
    });
    if (!ok) return;
    this.eventService.delete(item.id).subscribe({
      next: () => {
        this.toast.success('Event deleted.');
        this.selectedDetailItem.set(null);
        this.load();
      },
      error: err => this.toast.error(err.error?.message ?? 'Unable to delete this event.')
    });
  }

  // ---- Add Holiday ----

  private toDateStrLocal(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }

  openCreateHoliday(prefill?: Date, prefillName?: string): void {
    const base = prefill ?? new Date();
    this.newHolidayName = prefillName ?? '';
    this.newHolidayDescription = '';
    this.newHolidayStartDate = this.toDateStrLocal(base);
    this.newHolidayEndDate = this.toDateStrLocal(base);
    this.showHolidayForm.set(true);
  }

  closeHolidayForm(): void {
    this.showHolidayForm.set(false);
  }

  submitHoliday(): void {
    if (!this.newHolidayName.trim() || !this.newHolidayStartDate || !this.newHolidayEndDate) return;
    this.savingHoliday.set(true);
    this.holidayService.create({
      name: this.newHolidayName.trim(),
      startDate: this.newHolidayStartDate,
      endDate: this.newHolidayEndDate,
      description: this.newHolidayDescription.trim() || undefined
    }).subscribe({
      next: () => {
        this.savingHoliday.set(false);
        this.showHolidayForm.set(false);
        this.toast.success('Holiday added - visible to the whole company.');
        this.load();
      },
      error: err => {
        this.savingHoliday.set(false);
        this.toast.error(err.error?.message ?? 'Unable to add this holiday.');
      }
    });
  }

  // ---- Add Year's Holidays (bulk) ----

  openBulkHolidayForm(): void {
    this.bulkYear = this.anchorDate().getFullYear();
    this.loadBulkYearItems();
    this.showBulkHolidayForm.set(true);
  }

  private loadBulkYearItems(): void {
    const existing = new Set(
      this.items().filter(i => i.type === 'HOLIDAY').map(i => i.startAt.slice(0, 10))
    );
    this.bulkYearItems = getFestivalsForYear(this.bulkYear).map(f => ({
      ...f,
      // Already-added dates start unchecked, so re-opening this list after adding some
      // holidays doesn't immediately try to re-create (and get rejected as duplicates) the
      // ones already on the calendar.
      selected: !existing.has(f.date)
    }));
  }

  onBulkYearChange(): void {
    this.loadBulkYearItems();
  }

  toggleBulkItem(index: number): void {
    this.bulkYearItems[index].selected = !this.bulkYearItems[index].selected;
  }

  closeBulkHolidayForm(): void {
    this.showBulkHolidayForm.set(false);
  }

  submitBulkHolidays(): void {
    const selected = this.bulkYearItems.filter(i => i.selected);
    if (selected.length === 0) return;

    this.savingBulkHolidays.set(true);
    const requests = selected.map(i => ({ name: i.name, startDate: i.date, endDate: i.date }));
    this.holidayService.bulkCreate(requests).subscribe({
      next: result => {
        this.savingBulkHolidays.set(false);
        this.showBulkHolidayForm.set(false);
        if (result.failureCount === 0) {
          this.toast.success(`${result.successCount} holiday(s) added for ${this.bulkYear}.`);
        } else {
          this.toast.warning(`${result.successCount} added, ${result.failureCount} skipped (likely already existed or were in the past).`);
        }
        this.load();
      },
      error: err => {
        this.savingBulkHolidays.set(false);
        this.toast.error(err.error?.message ?? 'Unable to add these holidays.');
      }
    });
  }
}
