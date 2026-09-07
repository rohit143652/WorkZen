import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { FeatureStateService } from '../services/feature-state.service';
import { ToastService } from '../../shared/services/toast.service';

/**
 * Company Feature Configuration is the upper-level control (see FeatureAccessService on the
 * backend) - this is permissionGuard's sibling for that other axis. Reads a required feature
 * code (or list, ALL must be enabled - see app-shell's NavLeaf.feature for why AND, not OR) from
 * route data, e.g.:
 *   { path: 'my', data: { feature: ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE'] } }
 *
 * Hiding the sidebar link alone isn't enough - someone who bookmarked the URL, or just types it
 * directly, would otherwise still reach a page whose entire premise the Super Admin has switched
 * off for this company. This is UI/UX only, same caveat as permissionGuard: the backend
 * re-checks the real feature flag on every actual write regardless of what this allows through.
 */
export const featureGuard: CanActivateFn = (route) => {
  const featureState = inject(FeatureStateService);
  const router = inject(Router);
  const toast = inject(ToastService);

  const required: string | string[] | undefined = route.data?.['feature'];
  if (!required) return true;

  const codes = Array.isArray(required) ? required : [required];
  if (codes.every(code => featureState.isEnabled(code))) {
    return true;
  }

  toast.error("This feature isn't enabled for your company. Contact your Super Admin.");
  return router.createUrlTree(['/dashboard']);
};
