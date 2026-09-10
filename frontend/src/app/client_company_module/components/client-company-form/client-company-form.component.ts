import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClientCompanyService } from '../../services/client-company.service';
import { ClientCompanyResponse } from '../../models/client-company.model';
import { ToastService } from '../../../shared/services/toast.service';
import { SubscriptionPlanService } from '../../../subscription_module/services/subscription-plan.service';
import { SubscriptionPlan } from '../../../subscription_module/models/subscription.model';
import { calculateSubscriptionEndDate } from '../../../subscription_module/utils/subscription-date.util';

@Component({
  selector: 'app-client-company-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './client-company-form.component.html'
})
export class ClientCompanyFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clientCompanyService = inject(ClientCompanyService);
  private readonly planService = inject(SubscriptionPlanService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly plans = signal<SubscriptionPlan[]>([]);
  readonly selectedPlan = signal<SubscriptionPlan | null>(null);

  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly isEditMode = signal(false);
  readonly isViewOnly = signal(false);
  readonly companyId = signal<number | null>(null);
  readonly company = signal<ClientCompanyResponse | null>(null);

  readonly form = this.fb.nonNullable.group({
    companyCode: ['', Validators.required],
    companyName: ['', Validators.required],
    legalName: [''],
    email: ['', Validators.email],
    phone: [''],
    alternatePhone: [''],
    address: [''],
    city: [''],
    state: [''],
    country: [''],
    pincode: [''],
    contactPersonName: [''],
    contactPersonEmail: [''],
    contactPersonPhone: [''],
    createClientAdminLogin: [false],
    clientAdminLogin: this.fb.nonNullable.group({
      username: [''],
      password: ['']
    }),
    subscription: this.fb.nonNullable.group({
      planId: [null as number | null, Validators.required],
      billingCycle: ['MONTHLY', Validators.required],
      startDate: [new Date().toISOString().slice(0, 10), Validators.required],
      endDate: [''],
      status: ['ACTIVE', Validators.required],
      employeeLimitOverride: [null as number | null],
      monthlyPriceOverride: [null as number | null],
      yearlyPriceOverride: [null as number | null],
      notes: ['']
    })
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    const isNewRoute = this.route.snapshot.url.some(seg => seg.path === 'new');

    this.form.controls.createClientAdminLogin.valueChanges.subscribe(enabled => {
      const group = this.form.controls.clientAdminLogin;
      if (enabled) {
        group.controls.username.setValidators([Validators.required, Validators.minLength(3)]);
        group.controls.password.setValidators([Validators.required, Validators.minLength(8)]);
      } else {
        group.controls.username.clearValidators();
        group.controls.password.clearValidators();
      }
      group.controls.username.updateValueAndValidity();
      group.controls.password.updateValueAndValidity();
    });

    // Subscription is only relevant/required when CREATING - update() never reads it (see
    // backend ClientCompanyRequest javadoc), so it's disabled entirely outside the create flow
    // rather than asking an admin editing company details to also re-pick a plan every time.
    if (idParam) {
      this.form.controls.subscription.disable();
    } else {
      this.planService.list(true).subscribe({
        next: plans => this.plans.set(plans),
        error: () => this.toast.error('Unable to load subscription plans.')
      });
    }

    this.form.controls.subscription.controls.planId.valueChanges.subscribe(planId => {
      const plan = this.plans().find(p => p.id === planId) ?? null;
      this.selectedPlan.set(plan);
      const overrideControls = this.form.controls.subscription.controls;
      if (plan?.customEmployeeLimitAllowed) {
        overrideControls.employeeLimitOverride.setValidators([Validators.required, Validators.min(1)]);
        overrideControls.monthlyPriceOverride.setValidators([Validators.required, Validators.min(0)]);
        overrideControls.yearlyPriceOverride.setValidators([Validators.required, Validators.min(0)]);
      } else {
        overrideControls.employeeLimitOverride.clearValidators();
        overrideControls.monthlyPriceOverride.clearValidators();
        overrideControls.yearlyPriceOverride.clearValidators();
      }
      overrideControls.employeeLimitOverride.updateValueAndValidity();
      overrideControls.monthlyPriceOverride.updateValueAndValidity();
      overrideControls.yearlyPriceOverride.updateValueAndValidity();
    });

    // Auto-calculate End Date from Start Date + Billing Cycle - Monthly adds exactly one month,
    // Yearly adds exactly one year. Recomputed whenever either input changes; the admin can
    // still edit End Date manually afterward (e.g. for a negotiated custom period) without it
    // being overwritten again unless they change Start Date or Billing Cycle a second time.
    const recalcEndDate = () => {
      const sub = this.form.controls.subscription.controls;
      const computed = calculateSubscriptionEndDate(sub.startDate.value, sub.billingCycle.value);
      if (computed) sub.endDate.setValue(computed);
    };
    this.form.controls.subscription.controls.startDate.valueChanges.subscribe(recalcEndDate);
    this.form.controls.subscription.controls.billingCycle.valueChanges.subscribe(recalcEndDate);

    if (idParam) {
      this.companyId.set(Number(idParam));
      const isEditRoute = this.route.snapshot.url.some(seg => seg.path === 'edit');
      this.isEditMode.set(isEditRoute);
      this.isViewOnly.set(!isEditRoute);
      this.form.controls.companyCode.disable();
      this.loading.set(true);
      this.clientCompanyService.getById(Number(idParam)).subscribe({
        next: c => { this.company.set(c); this.patchForm(c); this.loading.set(false); },
        error: () => { this.toast.error('Unable to load client company.'); this.loading.set(false); }
      });
    } else if (isNewRoute) {
      this.isEditMode.set(false);
      this.form.controls.companyCode.disable();
      this.clientCompanyService.nextCode().subscribe({
        next: code => this.form.controls.companyCode.setValue(code),
        error: () => this.toast.error('Unable to generate the next company code.')
      });
    }
  }

  private patchForm(c: ClientCompanyResponse): void {
    this.form.patchValue({
      companyCode: c.companyCode,
      companyName: c.companyName,
      legalName: c.legalName ?? '',
      email: c.email ?? '',
      phone: c.phone ?? '',
      alternatePhone: c.alternatePhone ?? '',
      address: c.address ?? '',
      city: c.city ?? '',
      state: c.state ?? '',
      country: c.country ?? '',
      pincode: c.pincode ?? '',
      contactPersonName: c.contactPersonName ?? '',
      contactPersonEmail: c.contactPersonEmail ?? '',
      contactPersonPhone: c.contactPersonPhone ?? ''
    });
    if (this.isViewOnly()) this.form.disable();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const isUpdate = this.companyId() !== null && this.isEditMode();
    const payload = {
      ...raw,
      clientAdminLogin: raw.createClientAdminLogin ? raw.clientAdminLogin : undefined,
      subscription: isUpdate ? undefined : (raw.subscription as any)
    };

    const action$ = isUpdate
      ? this.clientCompanyService.update(this.companyId()!, payload)
      : this.clientCompanyService.create(payload);

    action$.subscribe({
      next: () => {
        this.toast.success(isUpdate ? 'Client company updated successfully.' : 'Client company created successfully.');
        this.saving.set(false);
        this.router.navigateByUrl('/clients');
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to save client company.');
      }
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/clients');
  }
}
