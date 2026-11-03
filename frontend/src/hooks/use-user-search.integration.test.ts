import { renderHook, waitFor } from '@testing-library/react'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import { useUserSearchQuery } from '~/hooks/use-user-search'
import http from '~/lib/http'
import { createQueryWrapper } from '~/test/test-utils'

const originalAdapter = http.defaults.adapter

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

describe('user search query integration', () => {
  it('exposes loading then reads the HTTP-07 page response', async () => {
    let requestConfig: InternalAxiosRequestConfig | undefined
    let resolveRequest: (() => void) | undefined
    http.defaults.adapter = (config) =>
      new Promise((resolve) => {
        requestConfig = config
        resolveRequest = () =>
          resolve({
            data: {
              content: {
                data: [
                  {
                    id: 'u2',
                    username: 'an',
                    fullName: 'Nguyễn Văn An',
                    avatarFileId: null,
                    status: 'OFFLINE',
                    lastSeen: null
                  }
                ],
                totalPages: 2,
                totalElements: 21
              }
            },
            status: 200,
            statusText: 'OK',
            headers: {},
            config
          } satisfies AxiosResponse)
      })
    const { QueryWrapper } = createQueryWrapper()
    const { result } = renderHook(() => useUserSearchQuery('  an  ', 1), { wrapper: QueryWrapper })

    expect(result.current.isPending).toBe(true)
    await waitFor(() => expect(requestConfig).toBeDefined())
    resolveRequest?.()

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(requestConfig?.params).toMatchObject({ keyword: 'an', page: 1, size: 20 })
    expect(result.current.data).toMatchObject({
      data: [{ username: 'an' }],
      totalPages: 2,
      totalElements: 21
    })
  })
})
