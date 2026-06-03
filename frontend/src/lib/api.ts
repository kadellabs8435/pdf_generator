import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthRequest = error.config?.url?.includes('/auth/')
    const status = error.response?.status
    const hadAuthHeader = Boolean(error.config?.headers?.Authorization)
    const message = (error.response?.data as { message?: string } | undefined)?.message ?? ''

    // Backend returns 401 when JWT is missing/invalid; 403 when logged in but not allowed.
    const sessionExpired =
      status === 401 ||
      (status === 403 && hadAuthHeader && message !== 'Access denied')

    if (sessionExpired && !isAuthRequest && hadAuthHeader) {
      useAuthStore.getState().logout()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message
  }
  if (error instanceof Error) return error.message
  return 'Something went wrong'
}
