import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';
import { ToastService } from '../../shared/services/toast.service';

/**
 * Reads a required permission from route data, e.g.:
 *   { path: 'users/new', data: { permission: 'USER_CREATE' } }
 * Also accepts an array - passes if the user has ANY ONE of the listed permissions, e.g.:
 *   { path: 'leave-requests', data: { permission: ['LEAVE_REQUEST_READ', 'LEAVE_REQUEST_SELF_CREATE'] } }
 * This is UI/UX only - the backend re-checks every request via @PreAuthorize.
 */
export const permissionGuard: CanActivateFn = (route) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);
  const toast = inject(ToastService);

  const requiredPermission: string | string[] | undefined = route.data?.['permission'];
  if (!requiredPermission) return true;

  const permissions = Array.isArray(requiredPermission) ? requiredPermission : [requiredPermission];
  if (permissions.some(p => authState.hasPermission(p))) {
    return true;
  }

  // Previously this redirected silently, so a user who followed a stale link/bookmark (or typed
  // the URL directly) into a page they lack permission for just landed back on the dashboard
  // with no explanation at all. Now they get a clear reason instead of a mystery bounce.
  toast.error("You don't have permission to access this page. Please contact your administrator.");
  return router.createUrlTree(['/dashboard']);
};
