import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SubscriptionPlanService } from '../../services/subscription-plan.service';
import { SubscriptionPlan } from '../../models/subscription.model';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';

/** Super Admin only - list of all subscription plans (spec section 19). */
@Component({
  selector: 'app-plan-list',
  standalone: true,
  imports: [CommonModule, RouterLink, HasPermissionDirective],
  templateUrl: './plan-list.component.html'
})
export class PlanListComponent {
  private readonly planService = inject(SubscriptionPlanService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly plans = signal<SubscriptionPlan[]>([]);
  readonly loading = signal(false);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.planService.list().subscribe({
      next: plans => { this.plans.set(plans); this.loading.set(false); },
      error: () => { this.toast.error('Unable to load subscription plans.'); this.loading.set(false); }
    });
  }

  async toggleActive(plan: SubscriptionPlan): Promise<void> {
    if (plan.active && plan.clientCount > 0) {
      const ok = await this.confirmDialog.ask({
        title: 'Deactivate this plan?',
        message: `${plan.clientCount} client(s) currently subscribe to "${plan.planName}". Deactivating hides it from new signups but does not affect their existing subscription.`,
        confirmLabel: 'Deactivate'
      });
      if (!ok) return;
    }
    const action$ = plan.active ? this.planService.deactivate(plan.id) : this.planService.activate(plan.id);
    action$.subscribe({
      next: () => { this.toast.success(plan.active ? 'Plan deactivated.' : 'Plan activated.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to update this plan.')
    });
  }
}
