import { AxiosError, AxiosHeaders, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createHttpClient, setUnauthorizedHandler } from './http'

const rejectWithStatus = (status: number) => (config: InternalAxiosRequestConfig) => {
  const response: AxiosResponse = {
    data: {},
    status,
    statusText: String(status),
    headers: {},
    config
  }
  return Promise.reject(new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, undefined, response))
}

afterEach(() => {
  setUnauthorizedHandler(null)
})

describe('HTTP session handling', () => {
  it('redirects a protected request to login after a 401 response', async () => {
    const replace = vi.fn()
    const client = createHttpClient({ timeout: 1000, location: { pathname: '/chat', replace } })

    await expect(client.get('/api/users/me', { adapter: rejectWithStatus(401) })).rejects.toMatchObject({
      response: { status: 401 }
    })
    expect(replace).toHaveBeenCalledWith('/login')
  })

  it('prefers registered unauthorized handler over location replace', async () => {
    const replace = vi.fn()
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    const client = createHttpClient({ timeout: 1000, location: { pathname: '/chat', replace } })

    await expect(client.get('/api/users/me', { adapter: rejectWithStatus(401) })).rejects.toMatchObject({
      response: { status: 401 }
    })
    expect(handler).toHaveBeenCalled()
    expect(replace).not.toHaveBeenCalled()
  })

  it('does not redirect on logout 401 failures', async () => {
    const replace = vi.fn()
    const client = createHttpClient({ timeout: 1000, location: { pathname: '/chat', replace } })

    await expect(client.post('/api/auth/logout', {}, { adapter: rejectWithStatus(401) })).rejects.toBeTruthy()
    expect(replace).not.toHaveBeenCalled()
  })

  it('does not redirect login failures or forbidden responses', async () => {
    const replace = vi.fn()
    const client = createHttpClient({ timeout: 1000, location: { pathname: '/login', replace } })

    await expect(client.post('/api/auth/login', {}, { adapter: rejectWithStatus(401) })).rejects.toBeTruthy()
    await expect(client.get('/api/conversations/c1', { adapter: rejectWithStatus(403) })).rejects.toBeTruthy()
    expect(replace).not.toHaveBeenCalled()
  })

  it('lets the browser set a multipart boundary for FormData', async () => {
    const client = createHttpClient({ timeout: 1000 })
    let capturedHeaders: AxiosHeaders | undefined

    await client.post('/api/files', new FormData(), {
      headers: { 'Content-Type': 'application/json' },
      adapter: async (config) => {
        capturedHeaders = config.headers
        return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
      }
    })

    expect(capturedHeaders?.get('Content-Type')).not.toBe('application/json')
  })
})
