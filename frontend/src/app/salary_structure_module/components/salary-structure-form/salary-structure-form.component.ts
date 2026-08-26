import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SalaryStructureService } from '../../services/salary-structure.service';
import { SalaryComponentService } from '../../services/salary-component.service';
import { SalaryComponentResponse, CalculationType } from '../../models/salary-component.model';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-salary-structure-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './salary-structure-form.component.html'
})
export class SalaryStructureFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly structureService = inject(SalaryStructureService);
  private readonly componentService = inject(SalaryComponentService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly availableComponents = signal<SalaryComponentResponse[]>([]);
  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly isEditMode = signal(false);
  readonly structureId = signal<number | null>(null);

  readonly form = this.fb.nonNullable.group({
    structureCode: [''],
    structureName: ['', Validators.required],
    salaryType: ['MONTHLY' as 'MONTHLY' | 'DAILY' | 'HOURLY' | 'CONTRACT', Validators.required],
    description: [''],
    dailyRate: [null as number | null],
    hourlyRate: [null as number | null],
    effectiveFrom: ['', Validators.required],
    effectiveTo: [''],
    components: this.fb.array<ReturnType<typeof this.buildComponentRow>>([])
  });

  get componentRows(): FormArray {
    return this.form.controls.components;
  }

  constructor() {
    this.componentService.list(false).subscribe(list => this.availableComponents.set(list));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.structureId.set(Number(idParam));
      this.loading.set(true);
      this.form.controls.structureCode.disable();
      this.structureService.getById(Number(idParam)).subscribe({
        next: s => {
          this.form.patchValue({
            structureCode: s.structureCode,
            structureName: s.structureName,
            salaryType: s.salaryType ?? 'MONTHLY',
            description: s.description ?? '',
            dailyRate: s.dailyRate ?? null,
            hourlyRate: s.hourlyRate ?? null,
            effectiveFrom: s.effectiveFrom,
            effectiveTo: s.effectiveTo ?? ''
          });
          for (const c of s.components) {
            this.componentRows.push(this.buildComponentRow({
              salaryComponentId: c.salaryComponentId,
              calculationType: c.calculationType,
              amount: c.amount ?? null,
              percentage: c.percentage ?? null,
              displayOrder: c.displayOrder
            }));
          }
          this.loading.set(false);
        },
        error: () => { this.toast.error('Unable to load salary structure.'); this.loading.set(false); }
      });
    } else {
      this.form.controls.structureCode.disable();
      this.structureService.nextCode().subscribe({
        next: code => this.form.controls.structureCode.setValue(code),
        error: () => this.toast.error('Unable to generate the next structure code.')
      });
      this.addComponentRow();
    }
  }

  private buildComponentRow(initial?: {
    salaryComponentId: number | null; calculationType: CalculationType; amount: number | null;
    percentage: number | null; displayOrder: number;
  }) {
    return this.fb.nonNullable.group({
      salaryComponentId: [initial?.salaryComponentId ?? null as number | null, Validators.required],
      calculationType: [initial?.calculationType ?? 'FIXED' as CalculationType, Validators.required],
      amount: [initial?.amount ?? null as number | null],
      percentage: [initial?.percentage ?? null as number | null],
      displayOrder: [initial?.displayOrder ?? 0]
    });
  }

  addComponentRow(): void {
    this.componentRows.push(this.buildComponentRow());
  }

  removeComponentRow(index: number): void {
    this.componentRows.removeAt(index);
  }

  needsAmount(calcType: CalculationType): boolean {
    return calcType === 'FIXED' || calcType === 'MANUAL' || calcType === 'PER_DAY' || calcType === 'PER_HOUR';
  }

  needsPercentage(calcType: CalculationType): boolean {
    return calcType === 'PERCENTAGE_OF_BASIC' || calcType === 'PERCENTAGE_OF_GROSS';
  }

  /** Drives which extra rate field (dailyRate/hourlyRate) the template shows. */
  needsDailyRate(): boolean {
    return this.form.controls.salaryType.value === 'DAILY';
  }

  needsHourlyRate(): boolean {
    return this.form.controls.salaryType.value === 'HOURLY';
  }

  componentName(id: number | null): string {
    return this.availableComponents().find(c => c.id === id)?.componentName ?? '';
  }

  /**
   * Rough client-side preview only (mirrors the backend's resolution order
   * for FIXED/MANUAL/PER_DAY/PER_HOUR -> basic -> %-of-basic -> gross ->
   * %-of-gross). The backend recalculates authoritatively on save; this is
   * just so the form isn't a black box while building a structure.
   */
  get preview(): { gross: number; deductions: number; net: number } {
    const rows = this.componentRows.controls.map(g => g.getRawValue());
    const byId = new Map(this.availableComponents().map(c => [c.id, c]));
    const resolved = new Map<number, number>();

    rows.forEach((r, i) => {
      if (this.needsAmount(r.calculationType)) resolved.set(i, r.amount ?? 0);
    });

    let basic = 0;
    rows.forEach((r, i) => {
      const comp = byId.get(r.salaryComponentId!);
      if (comp?.componentType === 'EARNING' && comp.componentCode === 'BASIC' && resolved.has(i)) {
        basic += resolved.get(i)!;
      }
    });

    rows.forEach((r, i) => {
      if (resolved.has(i)) return;
      if (r.calculationType === 'PERCENTAGE_OF_BASIC') resolved.set(i, (basic * (r.percentage ?? 0)) / 100);
    });

    let grossSoFar = 0;
    rows.forEach((r, i) => {
      const comp = byId.get(r.salaryComponentId!);
      if (comp?.componentType === 'EARNING' && resolved.has(i)) grossSoFar += resolved.get(i)!;
    });

    rows.forEach((r, i) => {
      if (resolved.has(i)) return;
      if (r.calculationType === 'PERCENTAGE_OF_GROSS') resolved.set(i, (grossSoFar * (r.percentage ?? 0)) / 100);
    });

    let gross = 0, deductions = 0, reimbursements = 0;
    rows.forEach((r, i) => {
      const comp = byId.get(r.salaryComponentId!);
      const amount = resolved.get(i) ?? 0;
      if (comp?.componentType === 'EARNING') gross += amount;
      else if (comp?.componentType === 'DEDUCTION') deductions += amount;
      else if (comp?.componentType === 'REIMBURSEMENT') reimbursements += amount;
    });

    const net = gross - deductions + reimbursements;
    return {
      gross: Math.round(gross * 100) / 100,
      deductions: Math.round(deductions * 100) / 100,
      net: Math.round(net * 100) / 100
    };
  }

  submit(): void {
    if (this.form.invalid || this.componentRows.length === 0) {
      this.form.markAllAsTouched();
      if (this.componentRows.length === 0) this.toast.warning('Add at least one salary component.');
      return;
    }

    this.saving.set(true);
    const raw = this.form.getRawValue();
    const payload = {
      structureCode: raw.structureCode || undefined,
      structureName: raw.structureName,
      salaryType: raw.salaryType,
      description: raw.description || undefined,
      dailyRate: raw.salaryType === 'DAILY' ? raw.dailyRate : null,
      hourlyRate: raw.salaryType === 'HOURLY' ? raw.hourlyRate : null,
      effectiveFrom: raw.effectiveFrom,
      effectiveTo: raw.effectiveTo || null,
      components: raw.components.map(c => ({
        salaryComponentId: c.salaryComponentId!,
        calculationType: c.calculationType,
        amount: this.needsAmount(c.calculationType) ? c.amount : null,
        percentage: this.needsPercentage(c.calculationType) ? c.percentage : null,
        displayOrder: c.displayOrder
      }))
    };

    const id = this.structureId();
    const action$ = this.isEditMode() && id ? this.structureService.update(id, payload) : this.structureService.create(payload);
    action$.subscribe({
      next: (saved) => {
        this.toast.success('Salary structure saved successfully.');
        this.saving.set(false);
        this.router.navigate(['/salary-structures', saved.id]);
      },
      error: (err: HttpErrorResponse) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to save salary structure.'); }
    });
  }

  cancel(): void {
    const id = this.structureId();
    if (this.isEditMode() && id) {
      this.router.navigate(['/salary-structures', id]);
    } else {
      this.router.navigateByUrl('/salary-structures');
    }
  }
}
