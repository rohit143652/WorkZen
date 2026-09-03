import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
import { ToastService } from './shared/services/toast.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet></router-outlet>'
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private lastBackPressAt = 0;

  ngOnInit(): void {
    if (!Capacitor.isNativePlatform()) return;

    /**
     * Deliberately does NOT use window.history.back() - the raw browser history stack has one
     * entry per navigation EVER made (every sidebar menu click, every "view details" click,
     * etc.), not just "the previous logically-related screen". Back button replaying that whole
     * stack eventually walks all the way past login, which is exactly the confusing "back keeps
     * going and going" behavior this replaces.
     *
     * Instead: anywhere except the Dashboard, back always jumps straight to Dashboard (one hop,
     * not a chain through unrelated pages) - simple and predictable, and it can never reach the
     * login screen since Dashboard is the app's home once logged in. AT Dashboard, the usual
     * "press back again to exit" pattern applies.
     */
    App.addListener('backButton', () => {
      const atRoot = this.router.url === '/dashboard' || this.router.url === '/';

      if (!atRoot) {
        this.router.navigateByUrl('/dashboard');
        return;
      }

      const now = Date.now();
      if (now - this.lastBackPressAt < 2000) {
        App.exitApp();
      } else {
        this.lastBackPressAt = now;
        this.toast.warning('Press back again to exit.');
      }
    });
  }
}
