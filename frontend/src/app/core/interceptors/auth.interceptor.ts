import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../../login_module/services/auth.service';
import { TokenService } from '../services/token.service';

const AUTH_FREE_PATHS = ['/auth/login', '/auth/refresh'];

// Module-level (singleton for the app) state to prevent multiple
// simultaneous refresh calls from racing each other.
let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthFree = AUTH_FREE_PATHS.some(path => req.url.includes(path));
  const accessToken = tokenService.getAccessToken();

  const authorizedReq = !isAuthFree && accessToken
    ? req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
    : req;

  return next(authorizedReq).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isAuthFree) {
        return throwError(() => error);
      }

      if (!isRefreshing) {
        isRefreshing = true;
        refreshedToken$.next(null);

        return authService.refresh().pipe(
          switchMap(data => {
            isRefreshing = false;
            refreshedToken$.next(data.accessToken);
            const retried = req.clone({ setHeaders: { Authorization: `Bearer ${data.accessToken}` } });
            return next(retried);
          }),
          catchError(refreshError => {
            isRefreshing = false;
            authService.clearLocalSession();
            router.navigate(['/login'], { queryParams: { returnUrl: router.url }, replaceUrl: true });
            return throwError(() => refreshError);
          })
        );
      }

      // A refresh is already in flight: wait for it instead of firing another one.
      return refreshedToken$.pipe(
        filter((token): token is string => token !== null),
        take(1),
        switchMap(token => {
          const retried = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
          return next(retried);
        })
      );
    })
  );
};
