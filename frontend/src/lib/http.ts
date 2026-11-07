import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

const DEFAULT_TIMEOUT = 10_000
const LOGIN_ENDPOINT = '/api/auth/login'

type HttpLocation = {
  pathname: string
  replace: (url: string) => void
}

type CreateHttpClientOptions = {
  baseURL?: string
  timeout: number
  location?: HttpLocation
}

const isLoginRequest = (instance: AxiosInstance, config?: InternalAxiosRequestConfig) => {
  if (!config || config.method?.toUpperCase() !== 'POST') return false

  try {
    return new URL(instance.getUri(config), 'http://localhost').pathname === LOGIN_ENDPOINT
  } catch {
    return false
  }
}

export const createHttpClient = ({ baseURL, timeout, location }: CreateHttpClientOptions): AxiosInstance => {
  const instance = axios.create({
    baseURL,
    timeout,
    withCredentials: true
  })

  instance.interceptors.request.use((config) => {
    const isFormData = typeof FormData !== 'undefined' && config.data instanceof FormData

    if (isFormData) {
      config.headers.delete('Content-Type')
    } else if (!config.headers.has('Content-Type')) {
      config.headers.set('Content-Type', 'application/json')
    }

    return config
  })

  instance.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
      const shouldRedirect =
        error.response?.status === 401 && !isLoginRequest(instance, error.config) && location?.pathname !== '/login'

      if (shouldRedirect) {
        location?.replace('/login')
      }

      return Promise.reject(error)
    }
  )

  return instance
}

const envTimeout = Number(import.meta.env.VITE_TIMEOUT)
const timeout = Number.isFinite(envTimeout) && envTimeout > 0 ? envTimeout : DEFAULT_TIMEOUT
const browserLocation = typeof window === 'undefined' ? undefined : window.location

const http = createHttpClient({
  baseURL: import.meta.env.VITE_API_URL,
  timeout,
  location: browserLocation
})

export default http
