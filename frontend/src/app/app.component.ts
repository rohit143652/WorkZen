import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
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
  private readonly toast = inject(ToastService);
  private lastBackPressAt = 0;

  ngOnInit(): void {
    if (!Capacitor.isNativePlatform()) return;

    /**
     * Without this, Android's hardware/gesture back button falls through to its OS-level
     * default of closing the whole app immediately from ANY screen - jarring compared to every
     * other app, where back just steps through in-app navigation first. canGoBack reflects the
     * WebView's own history (which Angular's router already pushes entries into via pushState),
     * so this is really just "let normal browser-style back navigation happen, and only treat
     * truly leaving the app as a deliberate, confirmed action" rather than an accident.
     */
    App.addListener('backButton', ({ canGoBack }: { canGoBack: boolean }) => {
      if (canGoBack) {
        window.history.back();
        return;
      }

      // Already at the root of in-app navigation (e.g. Dashboard) - require two presses within
      // 2 seconds to actually exit, the same "press back again to exit" pattern almost every
      // Android app uses, instead of exiting on a single accidental press.
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
