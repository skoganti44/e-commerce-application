import axios from 'axios';

// Vite dev server proxies /api -> http://localhost:8080 (see vite.config.js).
// In production set VITE_API_BASE_URL to point at your Spring Boot host.
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

const client = axios.create({
  baseURL,
  headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
  timeout: 10000,
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    const message =
      err.response?.data?.error ||
      err.response?.data?.message ||
      err.message ||
      'Request failed';
    return Promise.reject(new Error(message));
  }
);

export default client;
