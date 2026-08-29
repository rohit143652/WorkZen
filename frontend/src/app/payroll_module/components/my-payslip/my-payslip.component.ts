import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PayrollService } from '../../services/payroll.service';
import { ToastService } from '../../../shared/services/toast.service';
import { extractBlobErrorMessage } from '../../../shared/utils/blob-error.util';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

@Component({
  selector: 'app-my-payslip',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-payslip.component.html'
})
export class MyPayslipComponent {
  private readonly payrollService = inject(PayrollService);
  private readonly toast = inject(ToastService);

  readonly months = MONTH_NAMES.map((label, index) => ({ label, value: index + 1 }));
  readonly years: number[] = (() => {
    const current = new Date().getFullYear();
    return [current, current - 1, current - 2];
  })();

  year = new Date().getFullYear();
  month = new Date().getMonth() + 1;
  readonly downloading = signal(false);

  download(): void {
    this.downloading.set(true);
    this.payrollService.downloadMyPayslip(this.year, this.month).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `Payslip-${this.month}-${this.year}.pdf`;
        anchor.click();
        window.URL.revokeObjectURL(url);
        this.downloading.set(false);
        this.toast.success('Payslip downloaded.');
      },
      error: err => {
        this.downloading.set(false);
        extractBlobErrorMessage(err, 'Unable to generate your payslip for this month - it may not have been approved yet.')
          .then(message => this.toast.error(message));
      }
    });
  }
}
