import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AttendanceService } from '../../services/attendance.service';
import { ToastService } from '../../../shared/services/toast.service';

const ALL_DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

/** Company-wide attendance policy config: office hours, grace period, half/full-day thresholds, weekly off. */
@Component({
  selector: 'app-attendance-rules',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './attendance-rules.component.html',
  styleUrl: './attendance-rules.component.css'
})
export class AttendanceRulesComponent {
  private readonly fb = inject(FormBuilder);
  private readonly attendanceService = inject(AttendanceService);
  private readonly toast = inject(ToastService);

  readonly allDays = ALL_DAYS;
  readonly loading = signal(true);
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    officeStartTime: ['09:30', Validators.required],
    officeEndTime: ['18:30', Validators.required],
    requiredWorkingMinutes: [480, Validators.required],
    halfDayMinMinutes: [240, Validators.required],
    fullDayMinMinutes: [420, Validators.required],
    lateGraceMinutes: [10, Validators.required],
    defaultBreakMinutes: [45, Validators.required],
    allowMultipleCheckin: [false],
    checkInSelfieRequired: [false],
    checkOutSelfieRequired: [false]
  });

  readonly selectedWeeklyOffDays = signal<Set<string>>(new Set(['SUNDAY']));

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.attendanceService.getRuleConfig().subscribe({
      next: config => {
        this.form.patchValue({
          officeStartTime: config.officeStartTime?.slice(0, 5),
          officeEndTime: config.officeEndTime?.slice(0, 5),
          requiredWorkingMinutes: config.requiredWorkingMinutes,
          halfDayMinMinutes: config.halfDayMinMinutes,
          fullDayMinMinutes: config.fullDayMinMinutes,
          lateGraceMinutes: config.lateGraceMinutes,
          defaultBreakMinutes: config.defaultBreakMinutes,
          allowMultipleCheckin: config.allowMultipleCheckin,
          checkInSelfieRequired: config.checkInSelfieRequired,
          checkOutSelfieRequired: config.checkOutSelfieRequired
        });
        this.selectedWeeklyOffDays.set(new Set((config.weeklyOffDays || '').split(',').map(s => s.trim()).filter(Boolean)));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  isDaySelected(day: string): boolean {
    return this.selectedWeeklyOffDays().has(day);
  }

  toggleDay(day: string): void {
    this.selectedWeeklyOffDays.update(set => {
      const next = new Set(set);
      if (next.has(day)) next.delete(day); else next.add(day);
      return next;
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    this.attendanceService.updateRuleConfig({
      officeStartTime: `${raw.officeStartTime}:00`,
      officeEndTime: `${raw.officeEndTime}:00`,
      requiredWorkingMinutes: raw.requiredWorkingMinutes,
      halfDayMinMinutes: raw.halfDayMinMinutes,
      fullDayMinMinutes: raw.fullDayMinMinutes,
      lateGraceMinutes: raw.lateGraceMinutes,
      defaultBreakMinutes: raw.defaultBreakMinutes,
      allowMultipleCheckin: raw.allowMultipleCheckin,
      checkInSelfieRequired: raw.checkInSelfieRequired,
      checkOutSelfieRequired: raw.checkOutSelfieRequired,
      weeklyOffDays: Array.from(this.selectedWeeklyOffDays()).join(',')
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Attendance rules updated.');
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to update attendance rules.');
      }
    });
  }
}
