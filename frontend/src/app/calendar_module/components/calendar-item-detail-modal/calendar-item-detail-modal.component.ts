import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CalendarItemResponse } from '../../models/calendar.model';
import { AuthStateService } from '../../../core/services/auth-state.service';

@Component({
  selector: 'app-calendar-item-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './calendar-item-detail-modal.component.html'
})
export class CalendarItemDetailModalComponent {
  readonly authState = inject(AuthStateService);

  @Input({ required: true }) item!: CalendarItemResponse;
  @Output() close = new EventEmitter<void>();
  @Output() edit = new EventEmitter<CalendarItemResponse>();
  @Output() delete = new EventEmitter<CalendarItemResponse>();

  get isHoliday(): boolean {
    return this.item.type === 'HOLIDAY';
  }

  canManageEvent(): boolean {
    return this.authState.hasPermission('EVENT_UPDATE') || this.authState.hasPermission('EVENT_DELETE');
  }

  canDeleteHoliday(): boolean {
    return this.authState.hasPermission('HOLIDAY_DELETE');
  }

  formatDateTime(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' });
  }

  formatDate(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
  }

  formatTime(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
  }
}
