import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmployeeService } from '../../services/employee.service';
import { EmployeeBulkImportResult } from '../../models/employee.model';
import { ToastService } from '../../../shared/services/toast.service';
import { extractBlobErrorMessage } from '../../../shared/utils/blob-error.util';

@Component({
  selector: 'app-employee-bulk-import',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './employee-bulk-import.component.html'
})
export class EmployeeBulkImportComponent {
  private readonly employeeService = inject(EmployeeService);
  private readonly toast = inject(ToastService);

  readonly downloadingTemplate = signal(false);
  readonly uploading = signal(false);
  readonly result = signal<EmployeeBulkImportResult | null>(null);
  selectedFile: File | null = null;

  downloadTemplate(): void {
    this.downloadingTemplate.set(true);
    this.employeeService.downloadImportTemplate().subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'employee-import-template.xlsx';
        anchor.click();
        window.URL.revokeObjectURL(url);
        this.downloadingTemplate.set(false);
      },
      error: err => {
        this.downloadingTemplate.set(false);
        extractBlobErrorMessage(err, 'Unable to download the template.').then(message => this.toast.error(message));
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.result.set(null);
  }

  upload(): void {
    if (!this.selectedFile) return;
    this.uploading.set(true);
    this.employeeService.bulkImport(this.selectedFile).subscribe({
      next: res => {
        this.uploading.set(false);
        this.result.set(res);
        if (res.failureCount === 0) {
          this.toast.success(`All ${res.successCount} employee(s) imported successfully.`);
        } else {
          this.toast.warning(`${res.successCount} imported, ${res.failureCount} row(s) failed - see details below.`);
        }
      },
      error: err => {
        this.uploading.set(false);
        this.toast.error(err.error?.message ?? 'Unable to import this file.');
      }
    });
  }
}
