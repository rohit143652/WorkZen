import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.workzen.app',
  appName: 'WORKZEN',
  // The new Angular "application" builder (used by this project) outputs to a "browser"
  // subfolder even without SSR enabled - this MUST point there, not just dist/<project>, or
  // Capacitor will bundle an empty/wrong folder into the APK.
  webDir: 'dist/workforce-auth-frontend/browser',
  server: {
    // Only relevant for `npx cap run android` live-reload during development against your own
    // machine's dev server - the actual packaged APK always calls environment.prod.ts's
    // apiUrl (the deployed Render backend) directly over the network, regardless of this.
    androidScheme: 'https'
  }
};

export default config;
