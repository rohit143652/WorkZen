import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.workzen.app',
  appName: 'WORKZEN',
  // The new Angular "application" builder (used by this project) outputs to a "browser"
  // subfolder even without SSR enabled - this MUST point there, not just dist/<project>, or
  // Capacitor will bundle an empty/wrong folder into the APK.
  webDir: 'dist/workforce-auth-frontend/browser',
  server: {
    // The app's own page loads from this scheme (Capacitor serves local content under
    // http(s)://localhost, never the real backend's domain). This MUST match the backend's own
    // scheme (currently plain HTTP, since it has no TLS certificate) - a page served over HTTPS
    // making requests to plain HTTP is "Mixed Content" and gets blocked by the WebView itself,
    // a browser-level security rule that's completely separate from (and not fixed by)
    // AndroidManifest's networkSecurityConfig cleartext exception. "localhost" is always treated
    // as a secure context by browsers/WebViews regardless of scheme, so switching this to 'http'
    // does NOT break secure-context-only APIs like camera/geolocation.
    //
    // If the backend ever moves to a real domain with a proper HTTPS certificate, switch this
    // back to 'https' and update environment.prod.ts's apiUrl to https:// at the same time.
    androidScheme: 'http'
  }
};

export default config;
