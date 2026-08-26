import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';

/**
 * Reads required roles from route data, e.g.:
 *   { path: 'users', data: { roles: ['SUPER_ADMIN', 'ADMIN'] } }
 * Access is granted if the user has ANY of the listed roles.
 * This is UI/UX only - the backend re-checks every request.
 */
export const roleGuard: CanActivateFn = (route) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  const requiredRoles: string[] = route.data?.['roles'] ?? [];
  if (requiredRoles.length === 0 || authState.hasAnyRole(requiredRoles)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
