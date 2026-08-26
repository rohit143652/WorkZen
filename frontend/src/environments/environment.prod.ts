export const environment = {
  production: true,
  // Frontend and backend are deployed as two SEPARATE Render services (separate domains) - a
  // relative '/api' path here would resolve against the frontend's OWN domain, which has no
  // backend behind it, causing every API call to fail. Must be the backend's full URL instead.
  // If your backend's Render URL is different from this, update it here to match exactly.
  apiUrl: 'https://workzen-i3bs.onrender.com/api'
};
