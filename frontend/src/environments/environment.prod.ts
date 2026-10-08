export const environment = {
  production: true,
  // Relative on purpose: the page and the API are served from the SAME address (Nginx forwards /api to the backend on
  // :8080), so no IP or domain is baked into the build - changing the server's address or name never needs a rebuild.
  // The Android app is the one exception: see environment.apk.ts.
  apiUrl: '/api'
};
