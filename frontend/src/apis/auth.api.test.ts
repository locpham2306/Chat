import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import authApi from '~/apis/auth.api'
import http from '~/lib/http'

const originalAdapter = http.defaults.adapter

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

describe('authApi.logout', () => {
  it('gửi POST /api/auth/logout không kèm body', async () => {
    let requestConfig: InternalAxiosRequestConfig | undefined

    http.defaults.adapter = async (config) => {
      requestConfig = config

      return {
        data: undefined,
        status: 204,
        statusText: 'No Content',
        headers: {},
        config
      } satisfies AxiosResponse
    }

    await authApi.logout()

    expect(requestConfig?.method).toBe('post')
    expect(requestConfig?.url).toBe('/api/auth/logout')
    expect(requestConfig?.data).toBeUndefined()
  })
})

describe('authApi.getProfile', () => {
  it('khai báo response HTTP-05 là ApiResponse<User>', async () => {
    let requestConfig: InternalAxiosRequestConfig | undefined
    http.defaults.adapter = async (config) => {
      requestConfig = config
      return {
        data: {
          code: '0',
          strCode: 'success',
          message: 'SUCCESSFUL',
          content: { id: 'user-1', username: 'huy' }
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config
      } satisfies AxiosResponse
    }

    const response = await authApi.getProfile()

    expect(requestConfig?.method).toBe('get')
    expect(requestConfig?.url).toBe('/api/users/me')
    expect(response.data.content.username).toBe('huy')
  })
})
