import axios from 'axios';

export const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8000';

export const api = axios.create({ baseURL: API_BASE });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = 'Bearer ' + token;
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export function apiErrorText(err: unknown, fallback: string): string {
  const e = err as { response?: { status?: number } } | null;
  if (!e?.response) {
    return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
  }
  const status = e.response.status;
  if (status === 401) return 'Tu sesión expiró o no es válida. Inicia sesión nuevamente.';
  if (status === 403) {
    return 'No tienes permisos para acceder a este módulo. Usa una cuenta del personal de la clínica.';
  }
  if (status && status >= 500) return 'El servidor encontró un error interno. Intenta nuevamente.';
  return fallback;
}
