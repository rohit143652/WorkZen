import { APP_INITIALIZER, ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { catchError, firstValueFrom, of } from 'rxjs';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './login_module/services/auth.service';
import { AuthStateService } from './core/services/auth-state.service';

/**
 * On full page load, try to silently restore the session via the
 * HttpOnly refresh-token cookie before the router activates any guards.
 * If it fails (no cookie, expired, etc.) the app simply starts logged out.
 */
function initializeAuth(authService: AuthService, authState: AuthStateService) {
  return () =>
    firstValueFrom(
      authService.refresh().pipe(
        catchError(() => of(null))
      )
    ).then(async result => {
      if (result) {
        await firstValueFrom(authService.fetchCurrentUser().pipe(catchError(() => of(null))));
      }
      authState.setInitializing(false);
    });
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(routes),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      deps: [AuthService, AuthStateService],
      multi: true
    }
  ]
};
