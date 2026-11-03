import { AxiosError, type AxiosResponse } from 'axios'
import { QueryClient } from '@tanstack/react-query'
import { afterEach, describe, expect, it, vi } from 'vitest'
import authApi from '~/apis/auth.api'
import { getProfile, logoutSession, profileQueryKey } from '~/hooks/use-auth'
import http from '~/lib/http'
import { useAuthStore } from '~/stores/use-auth-store'
import type { User } from '~/types/user.type'

const originalAdapter = http.defaults.adapter

const user: User = {
  id: 'user-1',
  username: 'huy',
  fullName: 'Đinh Việt Huy',
  avatarFileId: null,
  status: 'ONLINE',
  lastSeen: null
}

afterEach(() => {
  vi.restoreAllMocks()
  http.defaults.adapter = originalAdapter
  useAuthStore.getState().logout()
})

describe('logoutSession', () => {
  it('xóa user và cache dữ liệu theo phiên sau khi server thu hồi phiên', async () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(profileQueryKey, user)
    queryClient.setQueryData(['conversations', 'list'], [{ id: 'private-conversation' }])
    useAuthStore.getState().login(user)

    http.defaults.adapter = async (config) =>
      ({
        data: undefined,
        status: 204,
        statusText: 'No Content',
        headers: {},
        config
      }) satisfies AxiosResponse

    await logoutSession(queryClient)

    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().user).toBeNull()
    expect(queryClient.getQueryData(profileQueryKey)).toBeUndefined()
    expect(queryClient.getQueryData(['conversations', 'list'])).toBeUndefined()
  })

  it('xóa phiên client khi server báo phiên đã hết hạn', async () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(profileQueryKey, user)
    useAuthStore.getState().login(user)

    const unauthorized = new AxiosError('Unauthorized', AxiosError.ERR_BAD_REQUEST)
    unauthorized.response = { status: 401 } as AxiosResponse
    vi.spyOn(authApi, 'logout').mockRejectedValue(unauthorized)

    await logoutSession(queryClient)

    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(queryClient.getQueryData(profileQueryKey)).toBeUndefined()
  })

  it('giữ phiên client khi server logout gặp lỗi 5xx', async () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(profileQueryKey, user)
    useAuthStore.getState().login(user)

    http.defaults.adapter = async (config) => {
      const response: AxiosResponse = {
        data: undefined,
        status: 500,
        statusText: 'Internal Server Error',
        headers: {},
        config
      }

      throw new AxiosError('Server error', AxiosError.ERR_BAD_RESPONSE, config, undefined, response)
    }

    await expect(logoutSession(queryClient)).rejects.toMatchObject({ response: { status: 500 } })
    expect(useAuthStore.getState().isAuthenticated).toBe(true)
    expect(queryClient.getQueryData(profileQueryKey)).toEqual(user)
  })
})

describe('getProfile', () => {
  it('unwrap content của ApiResponse từ HTTP-05', async () => {
    http.defaults.adapter = async (config) =>
      ({
        data: {
          code: '0',
          strCode: 'success',
          message: 'SUCCESSFUL',
          content: user
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config
      }) satisfies AxiosResponse

    await expect(getProfile()).resolves.toEqual(user)
  })
})
