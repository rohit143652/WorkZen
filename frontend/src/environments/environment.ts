export const environment = {
  production: false,
  // Relative too: `ng serve` forwards /api to the backend through proxy.conf.json (default http://127.0.0.1:8080), so the
  // same code works on a laptop and on a server without editing any address here.
  apiUrl: '/api'
};
