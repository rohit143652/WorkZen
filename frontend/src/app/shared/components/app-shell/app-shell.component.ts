import { CommonModule } from '@angular/common';
import { Component, effect, ElementRef, HostListener, inject, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../login_module/services/auth.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ToastContainerComponent } from '../toast/toast.component';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { AttendanceService } from '../../../attendance_module/services/attendance.service';

interface NavLeaf {
  label: string;
  path: string;
  icon: string;
  /** A single permission, or a list - visible if the user has ANY one of them. Used for pages
      like Leave that merge into one menu entry regardless of whether someone has the
      self-service permission, the manage permission, or (commonly) both. */
  permission?: string | string[];
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
    // Setup/structure items - things a Client Admin configures once (or rarely changes),
    // before any day-to-day workforce/attendance/payroll work happens. Departments &
    // Designations moved here from Workforce - it's the same kind of one-time setup as
    // Sites, not an ongoing "manage my people" action like the rest of that group.
    label: 'Organization', icon: 'building',
    children: [
      { label: 'Client Companies', path: '/clients', icon: 'building', permission: 'CLIENT_COMPANY_READ' },
      { label: 'Sites', path: '/sites', icon: 'geo-alt', permission: 'SITE_READ' },
      { label: 'Departments & Designations', path: '/org-settings', icon: 'diagram-3', permission: 'DEPARTMENT_READ' }
    ]
  },
  {
    // Ongoing people management, in the order you'd actually touch them: add/manage an
    // employee, assign them to a site, and - eventually - process their exit.
    label: 'Workforce', icon: 'people',
    children: [
      { label: 'Employees', path: '/employees', icon: 'people', permission: 'EMPLOYEE_READ' },
      // Renamed from "Employee Assignments" - the page itself says "Select a site, choose
      // employees, and assign them to that location", so "Site Assignments" says at a
      // glance what direction the assignment goes (site <- employees), not just that some
      // kind of assignment happens.
      { label: 'Site Assignments', path: '/employee-assignments', icon: 'link-45deg', permission: 'EMPLOYEE_ASSIGNMENT_READ' },
      { label: 'Exit Management', path: '/employee-exits', icon: 'box-arrow-right', permission: 'EMPLOYEE_EXIT_READ' }
    ]
  },
  {
    label: 'Attendance', icon: 'clock-history',
    children: [
      { label: 'Mark Attendance', path: '/attendance', icon: 'check2-square', permission: 'ATTENDANCE_CREATE' },
      { label: 'Attendance History', path: '/attendance/history', icon: 'clock-history', permission: 'ATTENDANCE_READ' },
      { label: 'Monthly Attendance Report', path: '/attendance/monthly-report', icon: 'file-earmark-excel', permission: 'MONTHLY_PAYMENT_REPORT_EXPORT' },
      { label: 'Holiday Calendar', path: '/holidays', icon: 'calendar-heart', permission: 'HOLIDAY_READ' },
      { label: 'Leave Requests', path: '/leave-requests', icon: 'calendar-week', permission: ['LEAVE_REQUEST_READ', 'LEAVE_REQUEST_SELF_CREATE'] },
      { label: 'Paid Leave Settings', path: '/paid-leave/settings', icon: 'calendar2-check', permission: 'PAID_LEAVE_CONFIG_UPDATE' }
    ]
  },
  {
    // Bonus/Deduction/Advance/Loan modules are planned to join this group over time; the full
    // payroll register (Basic/DA, EPF/ESI/PT, Net Payment) now lives on the Monthly Attendance
    // & Payment Report itself (see Attendance group above) - one report, not two.
    //
    // Reordered to match the actual monthly flow: set up pay structures and components first,
    // configure payroll-wide settings, THEN run payroll - "My Payslip" moved to last since it's
    // a different audience entirely (an employee checking their own payslip, not an admin
    // doing payroll work) rather than a step in the admin's own sequence.
    label: 'Payroll', icon: 'cash-stack',
    children: [
      { label: 'Salary Structures', path: '/salary-structures', icon: 'cash-stack', permission: 'SALARY_STRUCTURE_READ' },
      { label: 'Salary Components', path: '/salary-components', icon: 'sliders', permission: 'SALARY_STRUCTURE_READ' },
      { label: 'Payroll Settings', path: '/payroll/settings', icon: 'gear', permission: 'PAYROLL_REGISTER_EXPORT' },
      { label: 'Payroll Processing', path: '/payroll/runs', icon: 'journal-check', permission: 'PAYROLL_RUN_READ' },
      { label: 'My Payslip', path: '/payroll/my-payslip', icon: 'receipt', permission: 'PAYSLIP_SELF_VIEW' }
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
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive, ToastContainerComponent, ConfirmDialogComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.css'
})
export class AppShellComponent {
  private readonly authService = inject(AuthService);
  private readonly attendanceService = inject(AttendanceService);
  private readonly router = inject(Router);

  readonly authState = inject(AuthStateService);
  /** Defaults closed on narrow (tablet/mobile) screens so the drawer doesn't cover the whole
      page on first load - always effectively "open" on desktop since there's no way to close it
      there (the toggle button is hidden entirely above 900px, see the component CSS). */
  readonly sidebarOpen = signal(typeof window === 'undefined' || window.innerWidth > 900);
  readonly profileMenuOpen = signal(false);
  @ViewChild('profileRoot') profileRoot?: ElementRef<HTMLElement>;

  /** Closes the profile dropdown on any click outside it - the dropdown's own content already
      stops its clicks from bubbling this far (see the template), so this only ever needs to
      check "was the click on the profile trigger/dropdown itself, or somewhere else on the
      page" using a plain DOM containment check against the profile root element. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.profileMenuOpen()) return;
    const target = event.target as Node;
    if (this.profileRoot && !this.profileRoot.nativeElement.contains(target)) {
      this.profileMenuOpen.set(false);
    }
  }
  readonly expandedGroups = signal<Set<string>>(new Set());
  /** Quick-find across every nav item - essential once the menu has 20+ entries across several
      groups; typing auto-expands any group with a match so results are visible without also
      having to manually open that section first. */
  readonly navSearch = signal('');

  readonly dashboardItem = DASHBOARD_ITEM;

  constructor() {
    // Auto-expand whichever group contains the route the user is currently on
    // (covers both first load and any programmatic navigation into a submenu
    // page, e.g. following a link from a card elsewhere in the app).
    this.expandGroupForCurrentUrl(this.router.url);
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => {
        this.expandGroupForCurrentUrl(e.urlAfterRedirects);
        this.navSearch.set('');
        // Auto-close the drawer after navigating on tablet/mobile, where it's an overlay - on
        // desktop this is a no-op since there's no way to close the sidebar there anyway.
        if (typeof window !== 'undefined' && window.innerWidth <= 900) {
          this.sidebarOpen.set(false);
        }
      });

    // Re-checks EVERY time the logged-in user changes (not just once, at component
    // construction) - AppShellComponent is the root authenticated layout, so it can easily
    // stay alive across a logout/login cycle if that transition doesn't fully tear the
    // component down. Without this being reactive, "already marked today" from the PREVIOUS
    // user's session could keep showing for whoever logs in next, even though they personally
    // haven't marked anything - a stale-state bug, not a real cross-employee data leak (the
    // backend always scopes strictly to whichever user's token made the request).
    effect(() => {
      const user = this.authState.currentUser();
      if (!user || !this.authState.hasPermission('ATTENDANCE_SELF_MARK')) {
        this.todayAttendanceMarked.set(false);
        return;
      }
      this.attendanceService.myTodayStatus().subscribe({
        next: status => this.todayAttendanceMarked.set(status !== null),
        error: () => this.todayAttendanceMarked.set(false)
      });
    });
  }

  // ---- "Mark My Attendance" - lives in the profile dropdown (see template) rather than its
  // own page, since a one-click self check-in is exactly the kind of thing someone wants
  // available from anywhere, not a destination you navigate to. ----
  readonly todayAttendanceMarked = signal(false);
  readonly showAttendanceConfirm = signal(false);
  readonly markingAttendance = signal(false);
  readonly attendanceMarkError = signal<string | null>(null);

  openMarkAttendanceConfirm(): void {
    this.profileMenuOpen.set(false);
    this.attendanceMarkError.set(null);
    this.showAttendanceConfirm.set(true);
  }

  closeAttendanceConfirm(): void {
    this.showAttendanceConfirm.set(false);
  }

  confirmMarkAttendance(): void {
    this.markingAttendance.set(true);
    this.attendanceMarkError.set(null);

    const submit = (latitude?: number, longitude?: number) => {
      this.attendanceService.markMine(latitude, longitude).subscribe({
        next: () => {
          this.markingAttendance.set(false);
          this.todayAttendanceMarked.set(true);
          this.showAttendanceConfirm.set(false);
        },
        error: err => {
          this.markingAttendance.set(false);
          this.attendanceMarkError.set(err.error?.message ?? 'Unable to mark your attendance.');
        }
      });
    };

    if (!navigator.geolocation) { submit(); return; }
    navigator.geolocation.getCurrentPosition(
      position => submit(position.coords.latitude, position.coords.longitude),
      () => submit(),
      { enableHighAccuracy: true, timeout: 8000 }
    );
  }

  get visibleGroups(): NavGroup[] {
    const term = this.navSearch().trim().toLowerCase();
    return NAV_GROUPS
      .map(group => ({
        ...group,
        children: group.children.filter(c => this.isVisible(c) && (!term || c.label.toLowerCase().includes(term)))
      }))
      .filter(group => group.children.length > 0);
  }

  get dashboardVisible(): boolean {
    const term = this.navSearch().trim().toLowerCase();
    return this.isVisible(this.dashboardItem) && (!term || this.dashboardItem.label.toLowerCase().includes(term));
  }

  isVisible(item: NavLeaf): boolean {
    if (!item.permission) return true;
    const permissions = Array.isArray(item.permission) ? item.permission : [item.permission];
    return permissions.some(p => this.authState.hasPermission(p));
  }

  isExpanded(label: string): boolean {
    // While searching, every group with a surviving match auto-expands, regardless of its
    // normal collapsed/expanded state - no point hiding the very result someone just typed for.
    if (this.navSearch().trim()) return true;
    return this.expandedGroups().has(label);
  }

  onNavSearchChange(value: string): void {
    this.navSearch.set(value);
  }

  clearNavSearch(): void {
    this.navSearch.set('');
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
