export const environment = {
  production: true,
  // Backend is deployed on its own server at this IP:port - a relative '/api' path here would
  // resolve against wherever the frontend itself is hosted, which has no backend behind it,
  // causing every API call to fail. Must be the backend's full URL instead. If the backend's
  // address ever changes again, update it here to match exactly.
  apiUrl: 'http://43.204.237.48:8080/api'
};
