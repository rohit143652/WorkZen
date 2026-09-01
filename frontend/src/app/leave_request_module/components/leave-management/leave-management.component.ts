import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { MyLeaveComponent } from '../my-leave/my-leave.component';
import { LeaveRequestsComponent } from '../leave-requests/leave-requests.component';

type LeaveTab = 'mine' | 'all';

/**
 * A role can plausibly have BOTH LEAVE_REQUEST_SELF_CREATE (apply for my own leave) and
 * LEAVE_REQUEST_READ/MANAGE (see and act on everyone's leave) - Site Supervisor is exactly
 * this case. Two separate sidebar entries for that one role was confusing (which one do I
 * click?), so this wraps both existing pages into ONE menu entry with tabs, showing only
 * the tab(s) that permission actually allows - someone with just one permission never even
 * sees a tab bar, they just get that one view directly.
 */
@Component({
  selector: 'app-leave-management',
  standalone: true,
  imports: [CommonModule, MyLeaveComponent, LeaveRequestsComponent],
  templateUrl: './leave-management.component.html'
})
export class LeaveManagementComponent {
  private readonly authState = inject(AuthStateService);

  readonly canSeeMine = this.authState.hasPermission('LEAVE_REQUEST_SELF_CREATE');
  readonly canSeeAll = this.authState.hasPermission('LEAVE_REQUEST_READ');
  readonly showTabs = this.canSeeMine && this.canSeeAll;

  // Default to whichever view is more likely to be someone's primary reason for being here -
  // if they can manage everyone's leave, that's probably it; otherwise their own requests.
  readonly activeTab = signal<LeaveTab>(this.canSeeAll ? 'all' : 'mine');

  setTab(tab: LeaveTab): void {
    this.activeTab.set(tab);
  }
}
