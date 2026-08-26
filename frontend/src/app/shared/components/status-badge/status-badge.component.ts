import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

export type BadgeKind = 'success' | 'warning' | 'danger' | 'info' | 'muted';

const ICON_BY_KIND: Record<BadgeKind, string> = {
  success: 'check-circle-fill',
  warning: 'exclamation-triangle-fill',
  danger: 'x-circle-fill',
  info: 'info-circle-fill',
  muted: 'dash-circle'
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [class]="'badge-' + kind"><i class="bi" [ngClass]="'bi-' + icon"></i>{{ label }}</span>`
})
export class StatusBadgeComponent {
  @Input() label = '';
  @Input() kind: BadgeKind = 'muted';

  get icon(): string {
    return ICON_BY_KIND[this.kind];
  }
}
