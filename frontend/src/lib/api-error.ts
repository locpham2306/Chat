import axios from 'axios'
import type { ApiError } from '~/types/api-error.type'

const DEFAULT_ERROR_MESSAGE = 'Không thể kết nối đến máy chủ. Vui lòng thử lại.'

export const getApiError = (error: unknown): ApiError | null => {
  if (!axios.isAxiosError<ApiError>(error)) return null

  return error.response?.data ?? null
}

export const getApiErrorMessage = (error: unknown): string | undefined => {
  if (!error) return undefined

  return getApiError(error)?.message ?? DEFAULT_ERROR_MESSAGE
}

export const isUnauthorizedError = (error: unknown): boolean => {
  return axios.isAxiosError(error) && error.response?.status === 401
}
