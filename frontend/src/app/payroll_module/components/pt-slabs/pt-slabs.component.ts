import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PayrollService } from '../../services/payroll.service';
import { ProfessionalTaxSlab, ProfessionalTaxSlabRequest } from '../../models/payroll.model';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

/**
 * Professional Tax slab management - only matters once Payroll Settings' PT Calculation Mode is
 * set to SLAB; a company on FLAT mode can ignore this page entirely. Slabs are effective-dated
 * (editing never mutates a currently-effective row in place - same convention as Payroll
 * Settings/Salary Structure) so a past payroll month's PT stays reproducible even if the slab
 * structure is changed later.
 */
@Component({
  selector: 'app-pt-slabs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pt-slabs.component.html'
})
export class PtSlabsComponent {
  private readonly payrollService = inject(PayrollService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly slabs = signal<ProfessionalTaxSlab[]>([]);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);

  readonly today = new Date().toISOString().slice(0, 10);

  state = '';
  minSalary: number | null = null;
  maxSalary: number | null = null;
  ptAmount: number | null = null;
  effectiveFrom = '';
  effectiveTo = '';

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.payrollService.getPtSlabs().subscribe({
      next: rows => { this.slabs.set(rows); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Unable to load Professional Tax slabs.'); }
    });
  }

  openAddForm(): void {
    this.editingId.set(null);
    this.state = '';
    this.minSalary = null;
    this.maxSalary = null;
    this.ptAmount = null;
    this.effectiveFrom = this.today;
    this.effectiveTo = '';
    this.showForm.set(true);
  }

  openEditForm(slab: ProfessionalTaxSlab): void {
    this.editingId.set(slab.id);
    this.state = slab.state ?? '';
    this.minSalary = slab.minSalary;
    this.maxSalary = slab.maxSalary;
    this.ptAmount = slab.ptAmount;
    this.effectiveFrom = slab.effectiveFrom;
    this.effectiveTo = slab.effectiveTo ?? '';
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
  }

  save(): void {
    if (this.minSalary == null || this.minSalary < 0) {
      this.toast.warning('Enter a valid minimum salary (0 or more).');
      return;
    }
    if (this.maxSalary != null && this.maxSalary <= this.minSalary) {
      this.toast.warning('Maximum salary must be greater than minimum salary, or left blank for an unbounded top slab.');
      return;
    }
    if (this.ptAmount == null || this.ptAmount < 0) {
      this.toast.warning('Enter a valid Professional Tax amount (0 or more).');
      return;
    }
    if (!this.effectiveFrom) {
      this.toast.warning('An effective-from date is required.');
      return;
    }

    const request: ProfessionalTaxSlabRequest = {
      state: this.state.trim() || null,
      minSalary: this.minSalary,
      maxSalary: this.maxSalary,
      ptAmount: this.ptAmount,
      effectiveFrom: this.effectiveFrom,
      effectiveTo: this.effectiveTo || null
    };

    this.saving.set(true);
    const editingId = this.editingId();
    const action = editingId != null
      ? this.payrollService.updatePtSlab(editingId, request)
      : this.payrollService.createPtSlab(request);

    action.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.toast.success(editingId != null ? 'Slab updated.' : 'Slab added.');
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to save this slab.');
      }
    });
  }

  async remove(slab: ProfessionalTaxSlab): Promise<void> {
    const confirmed = await this.confirmDialog.ask({
      title: 'Delete Professional Tax Slab',
      message: `Delete the slab for ₹${slab.minSalary} - ${slab.maxSalary ?? 'and above'}? This cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    });
    if (!confirmed) return;
    this.payrollService.deletePtSlab(slab.id).subscribe({
      next: () => { this.toast.success('Slab deleted.'); this.load(); },
      error: () => this.toast.error('Unable to delete this slab.')
    });
  }
}
