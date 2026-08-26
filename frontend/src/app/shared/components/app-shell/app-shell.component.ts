import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../login_module/services/auth.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ToastContainerComponent } from '../toast/toast.component';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';

interface NavLeaf {
  label: string;
  path: string;
  icon: string;
  permission?: string;
}

interface NavGroup {
  label: string;
  icon: string;
  children: NavLeaf[];
}

/**
 * Grouped as: a couple of always-relevant top-level links, then submenus
 * for Organization (tenant/site structure), Workforce (people + their
 * day-to-day), Attendance, and Administration (accounts/access/audit) -
 * splitting what used to be 12 flat sidebar links into 5 top-level entries.
 */
const NAV_GROUPS: NavGroup[] = [
  {
    label: 'Organization', icon: 'building',
    children: [
      { label: 'Client Companies', path: '/clients', icon: 'building', permission: 'CLIENT_COMPANY_READ' },
      { label: 'Sites', path: '/sites', icon: 'geo-alt', permission: 'SITE_READ' }
    ]
  },
  {
    label: 'Workforce', icon: 'people',
    children: [
      { label: 'Employees', path: '/employees', icon: 'people', permission: 'EMPLOYEE_READ' },
      { label: 'Departments & Designations', path: '/org-settings', icon: 'diagram-3', permission: 'DEPARTMENT_READ' },
      { label: 'Employee Assignments', path: '/employee-assignments', icon: 'link-45deg', permission: 'EMPLOYEE_ASSIGNMENT_READ' }
    ]
  },
  {
    label: 'Attendance', icon: 'clock-history',
    children: [
      { label: 'Mark Attendance', path: '/attendance', icon: 'check2-square', permission: 'ATTENDANCE_CREATE' },
      { label: 'Attendance History', path: '/attendance/history', icon: 'clock-history', permission: 'ATTENDANCE_READ' },
      { label: 'Monthly Payment Report', path: '/attendance/monthly-report', icon: 'file-earmark-excel', permission: 'MONTHLY_PAYMENT_REPORT_EXPORT' },
      { label: 'Paid Leave Settings', path: '/paid-leave/settings', icon: 'calendar2-check', permission: 'PAID_LEAVE_CONFIG_UPDATE' }
    ]
  },
  {
    // Bonus/Deduction/Advance/Loan/Payslip modules are planned to join
    // this group over time; the full payroll register (Basic/DA, EPF/ESI/PT,
    // Net Payment) now lives on the Monthly Attendance & Payment Report
    // itself (see Attendance group below) - one report, not two.
    label: 'Payroll', icon: 'cash-stack',
    children: [
      { label: 'Salary Structures', path: '/salary-structures', icon: 'cash-stack', permission: 'SALARY_STRUCTURE_READ' },
      { label: 'Salary Components', path: '/salary-components', icon: 'sliders', permission: 'SALARY_STRUCTURE_READ' },
      { label: 'Payroll Settings', path: '/payroll/settings', icon: 'gear', permission: 'PAYROLL_REGISTER_EXPORT' },
      { label: 'Payroll Processing', path: '/payroll/runs', icon: 'journal-check', permission: 'PAYROLL_RUN_READ' }
    ]
  },
  {
    label: 'Advances', icon: 'wallet2',
    children: [
      { label: 'Employee Advances', path: '/advances', icon: 'wallet2', permission: 'ADVANCE_READ' }
    ]
  },
  {
    label: 'Administration', icon: 'shield-lock',
    children: [
      { label: 'User Management', path: '/users', icon: 'person-badge', permission: 'USER_READ' },
      { label: 'Roles', path: '/roles', icon: 'shield-check', permission: 'ROLE_READ' },
      { label: 'Permissions', path: '/permissions', icon: 'key', permission: 'PERMISSION_READ' },
      { label: 'Audit Logs', path: '/audit-logs', icon: 'journal-text', permission: 'AUDIT_LOG_READ' }
    ]
  }
];

const DASHBOARD_ITEM: NavLeaf = { label: 'Dashboard', path: '/dashboard', icon: 'speedometer2', permission: 'DASHBOARD_VIEW' };

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToastContainerComponent, ConfirmDialogComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.css'
})
export class AppShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly authState = inject(AuthStateService);
  /** Defaults closed on narrow (tablet/mobile) screens so the drawer doesn't cover the whole
      page on first load - always effectively "open" on desktop since there's no way to close it
      there (the toggle button is hidden entirely above 900px, see the component CSS). */
  readonly sidebarOpen = signal(typeof window === 'undefined' || window.innerWidth > 900);
  readonly profileMenuOpen = signal(false);
  readonly expandedGroups = signal<Set<string>>(new Set());

  readonly dashboardItem = DASHBOARD_ITEM;

  constructor() {
    // Auto-expand whichever group contains the route the user is currently on
    // (covers both first load and any programmatic navigation into a submenu
    // page, e.g. following a link from a card elsewhere in the app).
    this.expandGroupForCurrentUrl(this.router.url);
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => {
        this.expandGroupForCurrentUrl(e.urlAfterRedirects);
        // Auto-close the drawer after navigating on tablet/mobile, where it's an overlay - on
        // desktop this is a no-op since there's no way to close the sidebar there anyway.
        if (typeof window !== 'undefined' && window.innerWidth <= 900) {
          this.sidebarOpen.set(false);
        }
      });
  }

  get visibleGroups(): NavGroup[] {
    return NAV_GROUPS
      .map(group => ({ ...group, children: group.children.filter(c => this.isVisible(c)) }))
      .filter(group => group.children.length > 0);
  }

  isVisible(item: NavLeaf): boolean {
    return !item.permission || this.authState.hasPermission(item.permission);
  }

  isExpanded(label: string): boolean {
    return this.expandedGroups().has(label);
  }

  toggleGroup(label: string): void {
    const set = new Set(this.expandedGroups());
    if (set.has(label)) set.delete(label); else set.add(label);
    this.expandedGroups.set(set);
  }

  private expandGroupForCurrentUrl(url: string): void {
    const match = NAV_GROUPS.find(group => group.children.some(c => url.startsWith(c.path)));
    if (match && !this.expandedGroups().has(match.label)) {
      this.expandedGroups.update(set => new Set(set).add(match.label));
    }
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  toggleProfileMenu(): void {
    this.profileMenuOpen.update(v => !v);
  }

  logout(): void {
    this.profileMenuOpen.set(false);
    this.authService.logout().subscribe({
      complete: () => this.router.navigateByUrl('/login')
    });
  }

  initials(): string {
    const user = this.authState.currentUser();
    if (!user) return '?';
    const first = user.firstName?.[0] ?? user.username[0];
    const last = user.lastName?.[0] ?? '';
    return (first + last).toUpperCase();
  }
}
