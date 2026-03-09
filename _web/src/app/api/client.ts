import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';

/* ═══════════════════════════════════════════════
   Result<T> — matches backend Result wrapper
   Backend uses: code='000000' for success, message, timestamp, data
   ═══════════════════════════════════════════════ */
export interface ApiResult<T> {
  code: string;
  message: string;
  timestamp: string;
  data: T;
}

const SUCCESS_CODE = '000000';

export class ApiError extends Error {
  code: string;
  constructor(code: string, message: string) {
    super(message);
    this.code = code;
    this.name = 'ApiError';
  }
}

/* ═══════════════════════════════════════════════
   Toast Event Bus — simple pub/sub for notifications
   ═══════════════════════════════════════════════ */
export type ToastType = 'error' | 'warning' | 'success' | 'info';
export interface ToastEvent {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
}

type ToastListener = (toast: ToastEvent) => void;
const toastListeners: ToastListener[] = [];

export function onToast(listener: ToastListener) {
  toastListeners.push(listener);
  return () => {
    const idx = toastListeners.indexOf(listener);
    if (idx >= 0) toastListeners.splice(idx, 1);
  };
}

export function emitToast(type: ToastType, message: string, duration = 5000) {
  const event: ToastEvent = { id: Date.now().toString(36) + Math.random().toString(36).slice(2), type, message, duration };
  toastListeners.forEach(l => l(event));
}

/* ═══════════════════════════════════════════════
   Axios Instance
   ═══════════════════════════════════════════════ */
const BASE_URL = process.env.API_BASE_URL || '/api/v1';

const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('intermodal_auth_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * Extract a human-readable message from a backend Result.
 * data can be: string | { errors: [{ code, message }] } | null
 */
function extractErrorMessage(result: ApiResult<unknown>): string {
  const d = result.data as any;
  if (typeof d === 'string' && d) return d;
  if (d && Array.isArray(d.errors) && d.errors.length > 0) {
    return d.errors.map((e: any) => e.message).join('; ');
  }
  return result.message || 'Operation failed.';
}

// Response interceptor: unwrap Result<T> + error handling
api.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    const result = response.data;
    // Backend returns code='000000' for success
    if (result && result.code && result.code !== SUCCESS_CODE) {
      const msg = extractErrorMessage(result);
      emitToast('error', msg);
      throw new ApiError(result.code, msg);
    }
    return response;
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    // Backend returned a structured error (400, 404, etc.)
    if (error.response?.data?.code) {
      const result = error.response.data;
      const msg = extractErrorMessage(result);
      emitToast('error', msg);
      throw new ApiError(result.code, msg);
    }
    if (error.response?.status === 401) {
      localStorage.removeItem('intermodal_auth_token');
      emitToast('error', 'Session expired. Please sign in again.');
      window.location.href = '/login';
      throw new ApiError('401', 'Session expired.');
    }
    if (error.response?.status === 403) {
      emitToast('error', 'You do not have permission for this action.');
      throw new ApiError('403', 'You do not have permission for this action.');
    }
    if (error.code === 'ECONNABORTED') {
      emitToast('warning', 'Request timed out.');
      throw new ApiError('408', 'Request timed out.');
    }
    if (!error.response) {
      emitToast('error', 'Cannot connect to server. Check your internet connection.');
      throw new ApiError('0', 'Cannot connect to server.');
    }
    emitToast('error', 'An unexpected error occurred.');
    throw new ApiError(
      String(error.response?.status || 500),
      'An unexpected error occurred.'
    );
  }
);

/* ═══════════════════════════════════════════════
   Typed Helpers
   ═══════════════════════════════════════════════ */
export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const response = await api.get<ApiResult<T>>(url, { params });
  return response.data.data;
}

export async function apiPost<T>(url: string, data?: unknown): Promise<T> {
  const response = await api.post<ApiResult<T>>(url, data);
  return response.data.data;
}

export async function apiPut<T>(url: string, data?: unknown): Promise<T> {
  const response = await api.put<ApiResult<T>>(url, data);
  return response.data.data;
}

export async function apiDelete<T>(url: string): Promise<T> {
  const response = await api.delete<ApiResult<T>>(url);
  return response.data.data;
}

export default api;
