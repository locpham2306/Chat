import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import { RegisterPage } from './register-page'
import http from '~/lib/http'
import { renderWithQueryClient } from '~/test/test-utils'

const originalAdapter = http.defaults.adapter

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

describe('RegisterPage accessibility', () => {
  it('names the form and focuses username after a delayed account conflict', async () => {
    let rejectRegistration: (() => void) | undefined
    http.defaults.adapter = (config) =>
      new Promise((_, reject) => {
        rejectRegistration = () => {
          const response = {
            data: {
              status: 409,
              code: 'CONFLICT',
              message: 'Tài khoản đã tồn tại',
              path: '/api/auth/register',
              timestamp: '2026-11-03T00:00:00Z',
              fieldErrors: []
            },
            status: 409,
            statusText: 'Conflict',
            headers: {},
            config
          } satisfies AxiosResponse
          reject(new AxiosError('Conflict', AxiosError.ERR_BAD_REQUEST, config, undefined, response))
        }
      })
    renderWithQueryClient(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    )
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Họ và tên'), 'Đinh Việt Huy')
    await user.type(screen.getByLabelText('Tài khoản'), 'huy')
    await user.type(screen.getByLabelText('Mật khẩu'), 'secret')

    expect(screen.getByRole('form', { name: 'Tạo tài khoản' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Đăng ký' }))
    expect(await screen.findByRole('button', { name: 'Đang đăng ký...' })).toBeDisabled()
    rejectRegistration?.()

    expect(await screen.findByText('Tài khoản đã tồn tại')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText('Tài khoản')).toHaveFocus())
  })

  it('focuses the first field after delayed registration field errors', async () => {
    let rejectRegistration: (() => void) | undefined
    http.defaults.adapter = (config) =>
      new Promise((_, reject) => {
        rejectRegistration = () => {
          const response = {
            data: {
              status: 422,
              code: 'VALIDATION_ERROR',
              message: 'Dữ liệu không hợp lệ',
              path: '/api/auth/register',
              timestamp: '2026-11-03T00:00:00Z',
              fieldErrors: [
                { field: 'fullName', message: 'Họ và tên không hợp lệ' },
                { field: 'username', message: 'Tài khoản không hợp lệ' }
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
    renderWithQueryClient(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    )
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Họ và tên'), 'Đinh Việt Huy')
    await user.type(screen.getByLabelText('Tài khoản'), 'huy')
    await user.type(screen.getByLabelText('Mật khẩu'), 'secret')

    await user.click(screen.getByRole('button', { name: 'Đăng ký' }))
    expect(await screen.findByRole('button', { name: 'Đang đăng ký...' })).toBeDisabled()
    rejectRegistration?.()

    expect(await screen.findByText('Họ và tên không hợp lệ')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText('Họ và tên')).toHaveFocus())
  })
})
