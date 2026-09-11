import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SuperAdminDashboardService } from '../../services/super-admin-dashboard.service';
import { SuperAdminDashboardSummary } from '../../models/subscription.model';

/** Super Admin's own dashboard - deliberately narrow: total companies and expiring subscriptions only. See backend ClientSubscriptionService.getDashboardSummary() javadoc for why nothing else belongs here. */
@Component({
  selector: 'app-super-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './super-admin-dashboard.component.html'
})
export class SuperAdminDashboardComponent {
  private readonly service = inject(SuperAdminDashboardService);
  readonly summary = signal<SuperAdminDashboardSummary | null>(null);
  readonly loading = signal(true);

  constructor() {
    this.service.getSummary().subscribe({
      next: s => { this.summary.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
