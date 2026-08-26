import { Directive, Input, TemplateRef, ViewContainerRef, effect, inject } from '@angular/core';
import { AuthStateService } from '../../core/services/auth-state.service';

/**
 * Usage: <button *appHasPermission="'USER_CREATE'">Create User</button>
 *
 * Frontend-only UX affordance - hides elements the user can't use.
 * The backend remains the source of truth and re-validates every request.
 */
@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly authState = inject(AuthStateService);

  private requiredPermission: string | null = null;
  private hasView = false;

  @Input() set appHasPermission(permission: string) {
    this.requiredPermission = permission;
    this.updateView();
  }

  constructor() {
    effect(() => {
      // Re-evaluate whenever the reactive permissions signal changes.
      this.authState.permissions();
      this.updateView();
    });
  }

  private updateView(): void {
    const allowed = !!this.requiredPermission && this.authState.hasPermission(this.requiredPermission);

    if (allowed && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!allowed && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}
