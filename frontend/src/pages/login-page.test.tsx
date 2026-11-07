import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { LoginPage } from './login-page'
import http from '~/lib/http'
import { renderWithQueryClient } from '~/test/test-utils'

const originalAdapter = http.defaults.adapter

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

const renderLogin = () =>
  renderWithQueryClient(
    <MemoryRouter initialEntries={['/login']}>
      <Routes>
        <Route path='/login' element={<LoginPage />} />
        <Route path='/chat' element={<div>Chat đã mở</div>} />
      </Routes>
    </MemoryRouter>
  )

const fillCredentials = async () => {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Tài khoản'), 'huy')
  await user.type(screen.getByLabelText('Mật khẩu'), 'secret')
  return user
}

describe('LoginPage', () => {
  it('shows validation messages without sending an invalid request', async () => {
    const adapter = vi.fn()
    http.defaults.adapter = adapter
    renderLogin()

    await userEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText('Vui lòng nhập tài khoản')).toBeInTheDocument()
    expect(screen.getByText('Vui lòng nhập mật khẩu')).toBeInTheDocument()
    expect(adapter).not.toHaveBeenCalled()
  })

  it('names the form and focuses the first field after a delayed server rejection', async () => {
    let rejectLogin: (() => void) | undefined
    http.defaults.adapter = (config) =>
      new Promise((_, reject) => {
        rejectLogin = () => {
          const response = {
            data: {
              status: 422,
              code: 'VALIDATION_ERROR',
              message: 'Dữ liệu không hợp lệ',
              path: '/api/auth/login',
              timestamp: '2026-11-03T00:00:00Z',
              fieldErrors: [
                { field: 'username', message: 'Tài khoản không hợp lệ' },
                { field: 'password', message: 'Mật khẩu không hợp lệ' }
              ]
            },
            status: 422,
            statusText: 'Unprocessable Entity',
            headers: {},
            config
          } satisfies AxiosResponse
          reject(new AxiosError('Validation failed', AxiosError.ERR_BAD_REQUEST, config, undefined, response))
        }
      })
    renderLogin()
    const user = await fillCredentials()

    expect(screen.getByRole('form', { name: 'Đăng nhập' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))
    expect(await screen.findByRole('button', { name: 'Đang đăng nhập...' })).toBeDisabled()
    rejectLogin?.()

    expect(await screen.findByText('Tài khoản không hợp lệ')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText('Tài khoản')).toHaveFocus())
  })

  it('uses high-contrast text for authentication errors', async () => {
    http.defaults.adapter = async (config) => {
      const response = {
        data: {
          status: 401,
          code: 'UNAUTHENTICATED',
          message: 'Unauthorized',
          path: '/api/auth/login',
          timestamp: '2026-11-03T00:00:00Z',
          fieldErrors: []
        },
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config
      } satisfies AxiosResponse
      throw new AxiosError('Unauthorized', AxiosError.ERR_BAD_REQUEST, config, undefined, response)
    }
    renderLogin()
    const user = await fillCredentials()

    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveClass('text-auth-error-foreground')
  })

  it('disables the form while login is pending', async () => {
    let resolveLogin: (() => void) | undefined
    http.defaults.adapter = (config) =>
      new Promise((resolve) => {
        resolveLogin = () =>
          resolve({
            data: { content: { message: 'OK' } },
            status: 200,
            statusText: 'OK',
            headers: {},
            config
          } satisfies AxiosResponse)
      })
    renderLogin()
    const user = await fillCredentials()

    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByRole('button', { name: 'Đang đăng nhập...' })).toBeDisabled()
    expect(screen.getByLabelText('Tài khoản')).toBeDisabled()
    resolveLogin?.()
    await screen.findByText('Chat đã mở')
  })

  it('navigates to chat after a successful login', async () => {
    http.defaults.adapter = async (config) => ({
      data: { content: { message: 'OK' } },
      status: 200,
      statusText: 'OK',
      headers: {},
      config
    })
    renderLogin()
    const user = await fillCredentials()

    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText('Chat đã mở')).toBeInTheDocument()
  })

  it('renders the account error returned by a 401 response', async () => {
    http.defaults.adapter = async (config) => {
      const response = {
        data: {
          status: 401,
          code: 'UNAUTHENTICATED',
          message: 'Unauthorized',
          path: '/api/auth/login',
          timestamp: '2026-11-03T00:00:00Z',
          fieldErrors: []
        },
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config
      } satisfies AxiosResponse
      throw new AxiosError('Unauthorized', AxiosError.ERR_BAD_REQUEST, config, undefined, response)
    }
    renderLogin()
    const user = await fillCredentials()

    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
    expect(await screen.findByRole('alert')).toHaveTextContent('Tài khoản hoặc mật khẩu không đúng')
  })
})
