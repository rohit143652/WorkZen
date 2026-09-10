import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SubscriptionPlanService, FeatureCategory } from '../../services/subscription-plan.service';
import { ToastService } from '../../../shared/services/toast.service';

/** Super Admin only - create/edit a subscription plan, including its default feature set (spec section 20). */
@Component({
  selector: 'app-plan-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './plan-form.component.html'
})
export class PlanFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly planService = inject(SubscriptionPlanService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly isEditMode = signal(false);
  readonly planId = signal<number | null>(null);
  readonly categories = signal<FeatureCategory[]>([]);
  /** code -> checked, kept separate from the reactive form since a dynamic set of checkboxes (one per feature code) is far simpler as a plain object than as a FormArray here. */
  readonly selectedFeatures = signal<Record<string, boolean>>({});

  readonly form = this.fb.nonNullable.group({
    planCode: ['', Validators.required],
    planName: ['', Validators.required],
    description: [''],
    customEmployeeLimitAllowed: [false],
    employeeLimit: [null as number | null],
    monthlyPrice: [null as number | null],
    yearlyPrice: [null as number | null],
    displayOrder: [0]
  });

  constructor() {
    this.planService.getFeatureCatalog().subscribe({
      next: cats => this.categories.set(cats),
      error: () => this.toast.error('Unable to load the feature catalog.')
    });

    this.form.controls.customEmployeeLimitAllowed.valueChanges.subscribe(custom => {
      const limitCtrl = this.form.controls.employeeLimit;
      const monthlyCtrl = this.form.controls.monthlyPrice;
      const yearlyCtrl = this.form.controls.yearlyPrice;
      if (custom) {
        limitCtrl.clearValidators(); monthlyCtrl.clearValidators(); yearlyCtrl.clearValidators();
      } else {
        limitCtrl.setValidators([Validators.required, Validators.min(1)]);
        monthlyCtrl.setValidators([Validators.required, Validators.min(0)]);
        yearlyCtrl.setValidators([Validators.required, Validators.min(0)]);
      }
      limitCtrl.updateValueAndValidity(); monthlyCtrl.updateValueAndValidity(); yearlyCtrl.updateValueAndValidity();
    });
    // Trigger the validator logic once for the default (false) state.
    this.form.controls.customEmployeeLimitAllowed.updateValueAndValidity();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.planId.set(Number(idParam));
      this.loading.set(true);
      this.planService.getById(Number(idParam)).subscribe({
        next: plan => {
          this.form.patchValue({
            planCode: plan.planCode,
            planName: plan.planName,
            description: plan.description ?? '',
            customEmployeeLimitAllowed: plan.customEmployeeLimitAllowed,
            employeeLimit: plan.employeeLimit,
            monthlyPrice: plan.monthlyPrice,
            yearlyPrice: plan.yearlyPrice,
            displayOrder: plan.displayOrder
          });
          const selected: Record<string, boolean> = {};
          plan.featureCodes.forEach(code => selected[code] = true);
          this.selectedFeatures.set(selected);
          this.loading.set(false);
        },
        error: () => { this.toast.error('Unable to load this plan.'); this.loading.set(false); }
      });
    }
  }

  toggleFeature(code: string, checked: boolean): void {
    this.selectedFeatures.update(current => ({ ...current, [code]: checked }));
  }

  isChecked(code: string): boolean {
    return !!this.selectedFeatures()[code];
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const featureCodes = Object.entries(this.selectedFeatures()).filter(([, v]) => v).map(([k]) => k);
    const payload = { ...raw, featureCodes };

    const action$ = this.isEditMode()
      ? this.planService.update(this.planId()!, payload)
      : this.planService.create(payload);

    action$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Plan updated successfully.' : 'Plan created successfully.');
        this.saving.set(false);
        this.router.navigateByUrl('/subscription-plans');
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to save this plan.');
      }
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/subscription-plans');
  }
}
