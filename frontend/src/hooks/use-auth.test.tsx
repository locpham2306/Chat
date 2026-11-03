import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { profileQueryKey, useLoginMutation, useUpdateProfileMutation } from './use-auth'
import { useAuthStore } from '~/stores/use-auth-store'
import { createQueryWrapper } from '~/test/test-utils'

const { authApiMock, userApiMock } = vi.hoisted(() => ({
  authApiMock: { login: vi.fn(), register: vi.fn(), logout: vi.fn(), getProfile: vi.fn() },
  userApiMock: { updateProfile: vi.fn() }
}))
vi.mock('~/apis/auth.api', () => ({ default: authApiMock }))
vi.mock('~/apis/user.api', () => ({ default: userApiMock }))

const oldUser = {
  id: 'u1',
  username: 'huy',
  fullName: 'Huy',
  avatarFileId: null,
  status: 'ONLINE' as const,
  lastSeen: null
}
const updatedUser = { ...oldUser, fullName: 'Đinh Việt Huy', avatarFileId: 'f1' }

describe('auth and profile mutations', () => {
  beforeEach(() => {
    authApiMock.login.mockReset()
    userApiMock.updateProfile.mockReset()
    useAuthStore.getState().logout()
  })

  it('exposes pending then resolves a successful login', async () => {
    let resolveLogin!: (value: unknown) => void
    authApiMock.login.mockReturnValue(new Promise((resolve) => (resolveLogin = resolve)))
    const { QueryWrapper } = createQueryWrapper()
    const { result } = renderHook(() => useLoginMutation(), { wrapper: QueryWrapper })

    act(() => result.current.mutate({ username: 'huy', password: 'secret' }))
    await waitFor(() => expect(result.current.isPending).toBe(true))

    resolveLogin({ data: { content: { message: 'Đăng nhập thành công' } } })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual({ message: 'Đăng nhập thành công' })
  })

  it('updates both profile cache and session store after saving the profile', async () => {
    userApiMock.updateProfile.mockResolvedValue({ data: { content: updatedUser } })
    const { QueryWrapper, queryClient } = createQueryWrapper()
    queryClient.setQueryData(profileQueryKey, oldUser)
    useAuthStore.getState().login(oldUser)
    const { result } = renderHook(() => useUpdateProfileMutation(), { wrapper: QueryWrapper })

    await act(() => result.current.mutateAsync({ fullName: 'Đinh Việt Huy', avatarFileId: 'f1' }))

    expect(userApiMock.updateProfile).toHaveBeenCalledWith({ fullName: 'Đinh Việt Huy', avatarFileId: 'f1' })
    expect(queryClient.getQueryData(profileQueryKey)).toEqual(updatedUser)
    expect(useAuthStore.getState().user).toEqual(updatedUser)
  })

  it('keeps the previous session cache after a forbidden profile update', async () => {
    const forbidden = { isAxiosError: true, response: { status: 403 } }
    userApiMock.updateProfile.mockRejectedValue(forbidden)
    const { QueryWrapper, queryClient } = createQueryWrapper()
    queryClient.setQueryData(profileQueryKey, oldUser)
    useAuthStore.getState().login(oldUser)
    const { result } = renderHook(() => useUpdateProfileMutation(), { wrapper: QueryWrapper })

    await expect(act(() => result.current.mutateAsync({ fullName: 'Tên mới', avatarFileId: null }))).rejects.toBe(
      forbidden
    )

    expect(queryClient.getQueryData(profileQueryKey)).toEqual(oldUser)
    expect(useAuthStore.getState().user).toEqual(oldUser)
  })
})
