import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8081',
  timeout: 30000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status;
    const message = err.response?.data?.message;
    const requestId = err.response?.headers?.['x-request-id'] || err.response?.data?.requestId;

    if (process.env.NODE_ENV !== 'production' && requestId) {
      // eslint-disable-next-line no-console
      console.warn(`[API ${status}] requestId=${requestId}`, message || err.message);
    }

    if (status === 401 || (status === 403 && message === 'Authentication required')) {
      localStorage.clear();
      window.location.href = '/login';
      return Promise.reject(err);
    }

    // Keep session on ordinary 403 (authorization) — only clear on auth-required message above
    if (status === 409) {
      err.userMessage = message || 'Conflict: this resource already exists or violates a constraint.';
    } else if (status === 422) {
      err.userMessage = message || 'The request could not be processed.';
    } else if (status === 429) {
      err.userMessage = message || 'Too many requests. Please wait and try again.';
    } else if (status === 503) {
      err.userMessage = message || 'Service temporarily unavailable. Please try again shortly.';
    }

    return Promise.reject(err);
  }
);

export default api;
