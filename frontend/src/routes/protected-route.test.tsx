import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProtectedRoute } from './protected-route'

const { profileQueryMock } = vi.hoisted(() => ({ profileQueryMock: vi.fn() }))
vi.mock('~/hooks/use-auth', () => ({ useProfileQuery: profileQueryMock }))
vi.mock('~/hooks/use-websocket', () => ({ useWebSocketLifecycle: vi.fn() }))

const renderRoute = () =>
  render(
    <MemoryRouter initialEntries={['/chat']}>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path='/chat' element={<div>Chat đã sẵn sàng</div>} />
        </Route>
        <Route path='/login' element={<div>Màn hình đăng nhập</div>} />
      </Routes>
    </MemoryRouter>
  )

describe('ProtectedRoute session states', () => {
  beforeEach(() => profileQueryMock.mockReset())

  it('shows loading while checking the session', () => {
    profileQueryMock.mockReturnValue({ isPending: true, error: null, data: undefined })

    renderRoute()

    expect(screen.getByRole('status')).toHaveTextContent('Đang kiểm tra phiên đăng nhập...')
  })

  it('renders the protected route after session success', () => {
    profileQueryMock.mockReturnValue({
      isPending: false,
      isError: false,
      error: null,
      data: { id: 'u1', username: 'huy', fullName: 'Huy', avatarFileId: null, status: 'ONLINE', lastSeen: null }
    })

    renderRoute()

    expect(screen.getByText('Chat đã sẵn sàng')).toBeInTheDocument()
  })

  it('moves to login when the session endpoint returns 401', () => {
    profileQueryMock.mockReturnValue({
      isPending: false,
      isError: true,
      error: { isAxiosError: true, response: { status: 401 } },
      data: undefined
    })

    renderRoute()

    expect(screen.getByText('Màn hình đăng nhập')).toBeInTheDocument()
  })

  it('offers retry for a temporary session error', async () => {
    const refetch = vi.fn()
    profileQueryMock.mockReturnValue({
      isPending: false,
      isError: true,
      error: new Error('offline'),
      data: undefined,
      refetch
    })

    renderRoute()
    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(refetch).toHaveBeenCalledOnce()
  })
})
