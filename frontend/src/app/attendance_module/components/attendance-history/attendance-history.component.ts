import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AttendanceService } from '../../services/attendance.service';
import { ATTENDANCE_STATUSES, AttendanceResponse, AttendanceStatus } from '../../models/attendance.model';
import { EmployeeService } from '../../../employee_module/services/employee.service';
import { EmployeeResponse } from '../../../employee_module/models/employee.model';
import { StatusBadgeComponent, BadgeKind } from '../../../shared/components/status-badge/status-badge.component';
import { ToastService } from '../../../shared/services/toast.service';
import { EmployeeSearchSelectComponent } from '../../../employee_module/components/employee-search-select/employee-search-select.component';

function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-attendance-history',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent, EmployeeSearchSelectComponent],
  templateUrl: './attendance-history.component.html'
})
export class AttendanceHistoryComponent {
  private readonly attendanceService = inject(AttendanceService);
  private readonly employeeService = inject(EmployeeService);
  private readonly toast = inject(ToastService);

  readonly today = new Date().toISOString().slice(0, 10);
  fromDate = isoDaysAgo(30);
  toDate = this.today;
  selectedEmployeeId: number | null = null;

  readonly employees = signal<EmployeeResponse[]>([]);
  readonly records = signal<AttendanceResponse[]>([]);
  readonly loading = signal(false);
  readonly searched = signal(false);
  readonly statuses = ATTENDANCE_STATUSES;

  readonly editingId = signal<number | null>(null);
  readonly savingId = signal<number | null>(null);
  editStatus: AttendanceStatus = 'PRESENT';
  editRemarks = '';

  constructor() {
    this.employeeService.search({ status: 'ACTIVE', page: 0, size: 200 }).subscribe(res => this.employees.set(res.content));
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

  search(): void {
    if (!this.selectedEmployeeId) {
      this.toast.warning('Select an employee first.');
      return;
    }
    if (this.fromDate > this.toDate) {
      this.toast.warning('Start date must be on or before end date.');
      return;
    }
    this.loading.set(true);
    this.searched.set(true);
    this.attendanceService.forEmployee(this.selectedEmployeeId, this.fromDate, this.toDate).subscribe({
      next: records => { this.records.set(records); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Unable to load attendance history.'); }
    });
  }

  startEdit(record: AttendanceResponse): void {
    this.editingId.set(record.id);
    this.editStatus = record.status;
    this.editRemarks = record.remarks ?? '';
  }

  cancelEdit(): void {
    this.editingId.set(null);
  }

  saveEdit(record: AttendanceResponse): void {
    this.savingId.set(record.id);
    this.attendanceService.update(record.id, { status: this.editStatus, remarks: this.editRemarks || undefined }).subscribe({
      next: () => {
        this.toast.success('Attendance updated.');
        this.savingId.set(null);
        this.editingId.set(null);
        this.search();
      },
      error: err => {
        this.savingId.set(null);
        this.toast.error(err.error?.message ?? 'Unable to update attendance.');
      }
    });
  }
}
