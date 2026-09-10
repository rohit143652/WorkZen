import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClientSubscriptionService } from '../../services/client-subscription.service';
import { SubscriptionPlanService } from '../../services/subscription-plan.service';
import { ClientSubscription, SubscriptionHistoryEntry, SubscriptionPlan, SUBSCRIPTION_STATUSES } from '../../models/subscription.model';
import { calculateSubscriptionEndDate } from '../../utils/subscription-date.util';
import { ClientCompanyService } from '../../../client_company_module/services/client-company.service';
import { ClientCompanyResponse } from '../../../client_company_module/models/client-company.model';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

/** Super Admin only - view/change one client company's subscription, and its full history (spec section 33). */
@Component({
  selector: 'app-client-subscription',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './client-subscription.component.html'
})
export class ClientSubscriptionComponent {
  private readonly fb = inject(FormBuilder);
  private readonly subscriptionService = inject(ClientSubscriptionService);
  private readonly planService = inject(SubscriptionPlanService);
  private readonly clientCompanyService = inject(ClientCompanyService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly route = inject(ActivatedRoute);

  readonly companyId = Number(this.route.snapshot.paramMap.get('id'));
  readonly company = signal<ClientCompanyResponse | null>(null);
  readonly subscription = signal<ClientSubscription | null>(null);
  readonly history = signal<SubscriptionHistoryEntry[]>([]);
  readonly plans = signal<SubscriptionPlan[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly editing = signal(false);
  readonly statuses = SUBSCRIPTION_STATUSES;

  readonly form = this.fb.nonNullable.group({
    planId: [0, Validators.required],
    billingCycle: ['MONTHLY', Validators.required],
    startDate: ['', Validators.required],
    endDate: [''],
    status: ['ACTIVE', Validators.required],
    employeeLimitOverride: [null as number | null],
    monthlyPriceOverride: [null as number | null],
    yearlyPriceOverride: [null as number | null],
    notes: [''],
    reason: ['']
  });

  readonly selectedPlan = () => this.plans().find(p => p.id === this.form.controls.planId.value) ?? null;

  /** True when the admin is trying to switch to a different plan while staying Active AND the
      current (saved) subscription is still Active - this is exactly what the backend blocks
      (two-step flow: deactivate first, then assign the new plan separately) - shown proactively
      here so the admin sees why before clicking Save, not just after an error comes back. */
  readonly blockedBySameActivePlanSwitch = () => {
    const current = this.subscription();
    if (!current || current.status !== 'ACTIVE') return false;
    const switchingPlan = this.form.controls.planId.value !== current.planId;
    const stayingActive = this.form.controls.status.value === 'ACTIVE';
    return switchingPlan && stayingActive;
  };

  constructor() {
    this.load();
    this.planService.list(true).subscribe(plans => this.plans.set(plans));

    // Auto-calculate End Date from Start Date + Billing Cycle, same as the Add Client form -
    // the admin can still edit it manually afterward for a negotiated custom period.
    const recalcEndDate = () => {
      const computed = calculateSubscriptionEndDate(this.form.controls.startDate.value, this.form.controls.billingCycle.value);
      if (computed) this.form.controls.endDate.setValue(computed);
    };
    this.form.controls.startDate.valueChanges.subscribe(recalcEndDate);
    this.form.controls.billingCycle.valueChanges.subscribe(recalcEndDate);
  }

  load(): void {
    this.loading.set(true);
    this.clientCompanyService.getById(this.companyId).subscribe(c => this.company.set(c));
    this.subscriptionService.get(this.companyId).subscribe({
      next: s => {
        this.subscription.set(s);
        this.form.patchValue({
          planId: s.planId, billingCycle: s.billingCycle, startDate: s.startDate, endDate: s.endDate ?? '',
          status: s.status, employeeLimitOverride: s.employeeLimitOverride, monthlyPriceOverride: s.monthlyPriceOverride,
          yearlyPriceOverride: s.yearlyPriceOverride, notes: s.notes ?? ''
        }, { emitEvent: false });
        this.loading.set(false);
      },
      error: () => { this.toast.error('Unable to load this client\'s subscription.'); this.loading.set(false); }
    });
    this.subscriptionService.history(this.companyId).subscribe(h => this.history.set(h));
  }

  startEdit(): void {
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.load();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.blockedBySameActivePlanSwitch()) {
      this.toast.warning('Deactivate the current plan first (set Status away from Active), then assign the new plan as a separate step.');
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    this.subscriptionService.update(this.companyId, {
      ...raw,
      billingCycle: raw.billingCycle as any,
      status: raw.status as any,
      endDate: raw.endDate || null
    }).subscribe({
      next: () => {
        this.toast.success('Subscription updated successfully.');
        this.saving.set(false);
        this.editing.set(false);
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to update this subscription.');
      }
    });
  }

  /** Manual "deactivate now" - distinct from the automatic daily job that expires a subscription once its end date passes on its own. */
  async deactivateNow(): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Deactivate this subscription?',
      message: `This immediately restricts ${this.company()?.companyName ?? 'this client'}'s access according to the Cancelled/deactivated policy. This can be reversed afterward by assigning a new plan.`,
      confirmLabel: 'Deactivate Now'
    });
    if (!ok) return;
    this.subscriptionService.deactivate(this.companyId).subscribe({
      next: () => { this.toast.success('Subscription deactivated.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to deactivate this subscription.')
    });
  }
}
