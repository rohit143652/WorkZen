import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';

/**
 * Opposite of authGuard: keeps an already-logged-in user OFF the login page. Without this,
 * navigating back (browser back button/gesture) after logging in could land the user back on
 * the login screen even though their session is still valid - this bounces them straight to
 * the dashboard instead. Paired with `replaceUrl: true` on the post-login navigation (which
 * stops /login from sitting in the browser history in the first place), back should no longer
 * be able to reach the login screen at all while a session is active.
 */
export const guestGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (authState.isAuthenticated()) {
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};
