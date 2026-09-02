import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EventService } from '../../services/calendar.service';
import { CalendarItemResponse, EventRequest, EventVisibility } from '../../models/calendar.model';
import { EmployeeService } from '../../../employee_module/services/employee.service';
import { EmployeeResponse } from '../../../employee_module/models/employee.model';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-event-form-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './event-form-modal.component.html',
  styleUrl: './event-form-modal.component.css'
})
export class EventFormModalComponent implements OnChanges {
  private readonly eventService = inject(EventService);
  private readonly employeeService = inject(EmployeeService);
  private readonly toast = inject(ToastService);

  @Input() editingEvent: CalendarItemResponse | null = null;
  @Input() prefillStart: Date | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  readonly saving = signal(false);
  readonly activeEmployees = signal<EmployeeResponse[]>([]);
  readonly participantSearch = signal('');
  readonly showParticipantDropdown = signal(false);

  title = '';
  description = '';
  location = '';
  startDate = '';
  startTime = '';
  endDate = '';
  endTime = '';
  allDay = false;
  visibility: EventVisibility = 'SELECTED_USERS';
  selectedParticipantIds = new Set<number>();

  /** Only UNSELECTED, matching employees - once someone's added as a chip they drop out of the
      dropdown entirely (Zoho-style), so there's no confusing "already picked but still showing"
      state to scroll past. */
  get filteredEmployees(): EmployeeResponse[] {
    const term = this.participantSearch().trim().toLowerCase();
    const notYetSelected = this.activeEmployees().filter(e => !this.selectedParticipantIds.has(e.id));
    if (!term) return notYetSelected;
    return notYetSelected.filter(e =>
      e.employeeCode.toLowerCase().includes(term) || `${e.firstName} ${e.lastName}`.toLowerCase().includes(term)
    );
  }

  get selectedParticipants(): EmployeeResponse[] {
    return this.activeEmployees().filter(e => this.selectedParticipantIds.has(e.id));
  }

  ngOnChanges(): void {
    if (this.activeEmployees().length === 0) {
      this.employeeService.search({ status: 'ACTIVE', size: 500, sort: 'employeeCode,asc' }).subscribe(page => {
        this.activeEmployees.set(page.content);
      });
    }

    if (this.editingEvent) {
      const e = this.editingEvent;
      this.title = e.title;
      this.description = e.description ?? '';
      this.location = e.location ?? '';
      this.allDay = e.allDay;
      this.visibility = (e.visibility as EventVisibility) ?? 'SELECTED_USERS';
      this.selectedParticipantIds = new Set(e.participantEmployeeIds ?? []);
      const start = new Date(e.startAt);
      const end = new Date(e.endAt);
      this.startDate = this.toDateStr(start);
      this.startTime = this.toTimeStr(start);
      this.endDate = this.toDateStr(end);
      this.endTime = this.toTimeStr(end);
    } else {
      const base = this.prefillStart ?? new Date();
      const endBase = new Date(base.getTime() + 60 * 60000); // default 1-hour duration
      this.title = '';
      this.description = '';
      this.location = '';
      this.allDay = false;
      this.visibility = 'SELECTED_USERS';
      this.selectedParticipantIds = new Set();
      this.startDate = this.toDateStr(base);
      this.startTime = this.toTimeStr(base);
      this.endDate = this.toDateStr(endBase);
      this.endTime = this.toTimeStr(endBase);
    }
  }

  private toDateStr(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }

  private toTimeStr(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  toggleParticipant(id: number): void {
    if (this.selectedParticipantIds.has(id)) this.selectedParticipantIds.delete(id);
    else this.selectedParticipantIds.add(id);
  }

  addParticipant(employee: EmployeeResponse): void {
    this.selectedParticipantIds.add(employee.id);
    this.participantSearch.set('');
    // Deliberately stays open (unlike a single-select dropdown) so picking several people in a
    // row - the whole point of a multi-select - doesn't mean reopening the dropdown every time.
    this.showParticipantDropdown.set(true);
  }

  removeParticipant(id: number): void {
    this.selectedParticipantIds.delete(id);
  }

  onParticipantSearchFocus(): void {
    this.showParticipantDropdown.set(true);
  }

  onParticipantSearchBlur(): void {
    // Delayed so a click on a dropdown option registers BEFORE the dropdown closes - matches
    // the same reasoning as EmployeeSearchSelectComponent.onBlur().
    setTimeout(() => this.showParticipantDropdown.set(false), 150);
  }

  submit(): void {
    if (!this.title.trim() || !this.startDate || !this.endDate) return;
    if (this.visibility === 'SELECTED_USERS' && this.selectedParticipantIds.size === 0) {
      this.toast.error('Select at least one participant, or switch visibility to All Users.');
      return;
    }

    const startAt = this.allDay ? `${this.startDate}T00:00:00` : `${this.startDate}T${this.startTime || '00:00'}:00`;
    const endAt = this.allDay ? `${this.endDate}T23:59:59` : `${this.endDate}T${this.endTime || '00:00'}:00`;
    if (endAt < startAt) {
      this.toast.error('End time cannot be before start time.');
      return;
    }

    const request: EventRequest = {
      title: this.title.trim(),
      description: this.description.trim() || undefined,
      location: this.location.trim() || undefined,
      startAt, endAt,
      allDay: this.allDay,
      visibility: this.visibility,
      participantEmployeeIds: this.visibility === 'SELECTED_USERS' ? Array.from(this.selectedParticipantIds) : undefined
    };

    this.saving.set(true);
    const call = this.editingEvent ? this.eventService.update(this.editingEvent.id, request) : this.eventService.create(request);
    call.subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success(this.editingEvent ? 'Event updated.' : 'Event created.');
        this.saved.emit();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to save this event.');
      }
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
