/**
 * Used ONLY by the Android app build:  ng build --configuration=apk
 *
 * The packaged app does not load from the backend's own address (it runs from a local origin inside the phone), so it
 * cannot use a relative '/api' like the website does - it needs the server's full address.
 *
 * SERVER_HOST is a placeholder ON PURPOSE. The APK steps in the Production Deployment Guide replace it with the real
 * server address before building (just the host: the app reaches the API through Nginx on port 80). An APK built by mistake without that step then fails visibly (unknown host) instead
 * of silently talking to some old server.
 */
export const environment = {
  production: true,
  apiUrl: 'http://SERVER_HOST/api'
};
