import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';

/**
 * Reads a required permission from route data, e.g.:
 *   { path: 'users/new', data: { permission: 'USER_CREATE' } }
 * This is UI/UX only - the backend re-checks every request via @PreAuthorize.
 */
export const permissionGuard: CanActivateFn = (route) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  const requiredPermission: string | undefined = route.data?.['permission'];
  if (!requiredPermission || authState.hasPermission(requiredPermission)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
