import { Component, OnInit, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';

const DASHBOARD_URL = '/dashboard';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet></router-outlet>'
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.setupWebBackTrap();

    if (Capacitor.isNativePlatform()) {
      this.setupNativeBackHandling();
    }
  }

  /**
   * Makes Dashboard a hard floor for the browser/WebView's own back gesture: no matter how many
   * times back is pressed, once the user is on Dashboard they stay there - never bounced to
   * /login, never out of the app entirely.
   *
   * guestGuard (see core/guards/guest.guard.ts) already blocks any attempt to reach /login while
   * authenticated, so on its own that already stops most "back to login" cases. But a raw
   * browser back-press is a POPSTATE event the browser fires BEFORE Angular's router (and
   * therefore guestGuard) gets a chance to react to it - for one frame, the address bar/WebView
   * has already moved to whatever URL preceded Dashboard (which could be a route Angular has no
   * guard for at all, e.g. something before the SPA ever loaded). Re-pushing a fresh history
   * entry the instant we detect we're on Dashboard closes that gap completely: every future back
   * press just consumes that entry and lands back on Dashboard again, so there's nothing further
   * back TO go to, regardless of how many times it's pressed.
   */
  private setupWebBackTrap(): void {
    const sealDashboard = () => {
      if (this.router.url === DASHBOARD_URL) {
        history.pushState({ workzenDashboardFloor: true }, '', location.href);
      }
    };

    // Re-seal every time navigation lands on Dashboard (covers arriving there via login, via
    // the sidebar, or via back itself).
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe(e => {
      if (e.urlAfterRedirects === DASHBOARD_URL) {
        sealDashboard();
      }
    });

    // Also re-seal on every raw popstate while already on Dashboard, closing the one-frame gap
    // described above.
    window.addEventListener('popstate', sealDashboard);
  }

  /**
   * Native hardware/gesture back button: anywhere except Dashboard, back always jumps straight
   * to Dashboard (one hop, not a chain back through unrelated screens - deliberately not
   * window.history.back(), whose raw stack has one entry per navigation ever made and would
   * eventually walk all the way back past login). AT Dashboard, back is simply absorbed and does
   * nothing - the user stays there no matter how many times it's pressed; leaving the app is up
   * to the OS's own home/app-switcher gesture, not the back button.
   */
  private setupNativeBackHandling(): void {
    App.addListener('backButton', () => {
      if (this.router.url !== DASHBOARD_URL) {
        this.router.navigateByUrl(DASHBOARD_URL);
      }
      // At Dashboard: intentionally do nothing - back is absorbed, never exits the app.
    });
  }
}
