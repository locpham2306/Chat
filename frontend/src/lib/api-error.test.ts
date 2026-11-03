import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { expect, test } from 'vitest'
import { getApiError, getApiErrorMessage, isUnauthorizedError } from './api-error.ts'
import type { ApiError } from '~/types/api-error.type'

test('lấy payload lỗi API từ AxiosError', () => {
  const payload: ApiError = {
    timestamp: '2026-10-22T08:00:00Z',
    status: 401,
    code: 'UNAUTHENTICATED',
    message: 'Sai thông tin đăng nhập',
    path: '/api/auth/login',
    fieldErrors: []
  }
  const config = { headers: {} } as InternalAxiosRequestConfig
  const response = { data: payload, status: 401, statusText: 'Unauthorized', headers: {}, config } as AxiosResponse
  const error = new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, response)

  expect(getApiError(error)).toBe(payload)
  expect(getApiError(new Error('Lỗi khác'))).toBeNull()
})

test('hiển thị lỗi kết nối khi AxiosError không có response', () => {
  const error = new AxiosError('Network Error', 'ERR_NETWORK')

  expect(getApiErrorMessage(error)).toBe('Không thể kết nối đến máy chủ. Vui lòng thử lại.')
})

test('chỉ xem AxiosError có status 401 là phiên hết hạn', () => {
  const config = { headers: {} } as InternalAxiosRequestConfig
  const unauthorizedResponse = {
    data: undefined,
    status: 401,
    statusText: 'Unauthorized',
    headers: {},
    config
  } as AxiosResponse
  const serverErrorResponse = {
    data: undefined,
    status: 500,
    statusText: 'Internal Server Error',
    headers: {},
    config
  } as AxiosResponse

  expect(
    isUnauthorizedError(
      new AxiosError('Unauthorized', AxiosError.ERR_BAD_REQUEST, config, undefined, unauthorizedResponse)
    )
  ).toBe(true)
  expect(
    isUnauthorizedError(
      new AxiosError('Server error', AxiosError.ERR_BAD_RESPONSE, config, undefined, serverErrorResponse)
    )
  ).toBe(false)
  expect(isUnauthorizedError(new AxiosError('Network error', AxiosError.ERR_NETWORK))).toBe(false)
})
