import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';

/* ═══════════════════════════════════════════════
   Result<T> — matches backend Result wrapper
   ═══════════════════════════════════════════════ */
export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  success: boolean;
}

export class ApiError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.code = code;
    this.name = 'ApiError';
  }
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
    const token = localStorage.getItem('auth_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: unwrap Result<T> + error handling
api.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    const result = response.data;
    if (result && result.success === false) {
      throw new ApiError(result.code, result.message || 'İşlem başarısız.');
    }
    return response;
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    if (error.response?.data?.message) {
      throw new ApiError(
        error.response.data.code || error.response.status,
        error.response.data.message
      );
    }
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token');
      window.location.href = '/login';
      throw new ApiError(401, 'Oturum süresi doldu. Lütfen tekrar giriş yapın.');
    }
    if (error.response?.status === 403) {
      throw new ApiError(403, 'Bu işlem için yetkiniz yok.');
    }
    if (error.code === 'ECONNABORTED') {
      throw new ApiError(408, 'İstek zaman aşımına uğradı.');
    }
    if (!error.response) {
      throw new ApiError(0, 'Sunucuya bağlanılamıyor. İnternet bağlantınızı kontrol edin.');
    }
    throw new ApiError(
      error.response?.status || 500,
      'Beklenmeyen bir hata oluştu.'
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
