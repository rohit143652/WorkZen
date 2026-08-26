import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../../services/dashboard.service';
import { DashboardSummary } from '../../models/dashboard-summary.model';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { SiteService } from '../../../site_module/services/site.service';
import { SiteResponse } from '../../../site_module/models/site.model';
import { StatusBadgeComponent, BadgeKind } from '../../../shared/components/status-badge/status-badge.component';

interface SummaryCard {
  label: string;
  value: number;
  kind: 'primary' | 'success' | 'info' | 'warning' | 'muted';
  icon: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly siteService = inject(SiteService);
  readonly authState = inject(AuthStateService);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly cards = signal<SummaryCard[]>([]);
  readonly isGlobal = signal(false);

  readonly sites = signal<SiteResponse[]>([]);
  readonly loadingSites = signal(false);

  readonly hasAnalyticsPermission =
    this.authState.hasPermission('DASHBOARD_ANALYTICS') || this.authState.hasPermission('CLIENT_DASHBOARD_VIEW');
  readonly hasSiteReadPermission = this.authState.hasPermission('SITE_READ');

  constructor() {
    if (this.hasAnalyticsPermission) {
      this.load();
    } else {
      this.loading.set(false);
    }
    if (this.hasSiteReadPermission) {
      this.loadSites();
    }
  }

  private load(): void {
    this.loading.set(true);
    this.dashboardService.getSummary().subscribe({
      next: (summary: DashboardSummary) => {
        this.isGlobal.set(summary.global);
        this.cards.set(summary.global ? this.buildGlobalCards(summary) : this.buildTenantCards(summary));
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  private buildGlobalCards(s: DashboardSummary): SummaryCard[] {
    return [
      { label: 'Total Client Companies', value: s.totalClientCompanies, kind: 'primary', icon: 'building' },
      { label: 'Active Client Companies', value: s.activeClientCompanies, kind: 'success', icon: 'building-check' },
      { label: 'Total Employees', value: s.totalEmployees, kind: 'primary', icon: 'people' },
      { label: 'Total Sites', value: s.totalSites, kind: 'info', icon: 'geo-alt' }
    ];
  }

  private buildTenantCards(s: DashboardSummary): SummaryCard[] {
    return [
      { label: 'Total Employees', value: s.totalEmployees, kind: 'primary', icon: 'people' },
      { label: 'Active Employees', value: s.activeEmployees, kind: 'success', icon: 'person-check' },
      { label: 'Total Sites', value: s.totalSites, kind: 'info', icon: 'geo-alt' },
      { label: 'Unassigned Employees', value: s.unassignedEmployees, kind: 'warning', icon: 'exclamation-triangle' },
      { label: 'Locked Users', value: s.lockedUsers, kind: 'warning', icon: 'lock' }
    ];
  }

  private loadSites(): void {
    this.loadingSites.set(true);
    this.siteService.list(0, 100).subscribe({
      next: res => { this.sites.set(res.content); this.loadingSites.set(false); },
      error: () => this.loadingSites.set(false)
    });
  }

  allocationStatus(site: SiteResponse): { label: string; kind: BadgeKind } {
    if (site.requiredEmployeeCount <= 0) return { label: 'No target set', kind: 'muted' };
    if (site.assignedEmployeeCount > site.requiredEmployeeCount) return { label: 'Overallocated', kind: 'warning' };
    if (site.assignedEmployeeCount === site.requiredEmployeeCount) return { label: 'Full', kind: 'success' };
    return { label: 'Understaffed', kind: 'info' };
  }
}
