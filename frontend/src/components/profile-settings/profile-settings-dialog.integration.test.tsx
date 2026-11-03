import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProfileSettingsDialog } from './profile-settings-dialog'
import { renderWithQueryClient } from '~/test/test-utils'

const { fileApiMock, userApiMock } = vi.hoisted(() => ({
  fileApiMock: { upload: vi.fn() },
  userApiMock: { updateProfile: vi.fn() }
}))
vi.mock('~/apis/file.api', () => ({ default: fileApiMock }))
vi.mock('~/apis/user.api', () => ({ default: userApiMock }))

const profile = {
  id: 'u1',
  username: 'huy',
  fullName: 'Huy',
  avatarFileId: null,
  status: 'ONLINE' as const,
  lastSeen: null
}

const renderDialog = (onOpenChange = vi.fn()) => ({
  onOpenChange,
  ...renderWithQueryClient(<ProfileSettingsDialog open onOpenChange={onOpenChange} user={profile} />)
})

describe('ProfileSettingsDialog', () => {
  beforeEach(() => {
    fileApiMock.upload.mockReset()
    userApiMock.updateProfile.mockReset()
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:preview') })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() })
  })

  it('blocks saving a blank full name', async () => {
    renderDialog()

    await userEvent.clear(screen.getByLabelText(/Họ và tên/))

    expect(screen.getByText('Họ và tên không được để trống.')).toBeInTheDocument()
    expect(screen.getByLabelText(/Họ và tên/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText(/Họ và tên/)).toHaveAccessibleDescription('Họ và tên không được để trống.')
    expect(screen.getByText('Họ và tên không được để trống.')).toHaveAttribute('role', 'alert')
    expect(screen.getByRole('button', { name: 'Lưu thay đổi' })).toBeDisabled()
    expect(userApiMock.updateProfile).not.toHaveBeenCalled()
  })

  it('saves the complete profile body and closes after success', async () => {
    userApiMock.updateProfile.mockResolvedValue({
      data: { content: { ...profile, fullName: 'Đinh Việt Huy' } }
    })
    const { onOpenChange } = renderDialog()
    const fullName = screen.getByLabelText(/Họ và tên/)
    await userEvent.clear(fullName)
    await userEvent.type(fullName, '  Đinh Việt Huy  ')

    await userEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    await waitFor(() =>
      expect(userApiMock.updateProfile).toHaveBeenCalledWith({ fullName: 'Đinh Việt Huy', avatarFileId: null })
    )
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('keeps the dialog open and renders a forbidden update error', async () => {
    userApiMock.updateProfile.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 403,
        data: {
          status: 403,
          code: 'FORBIDDEN',
          message: 'Bạn không có quyền cập nhật hồ sơ.',
          path: '/api/users/me',
          timestamp: '2026-11-03T00:00:00Z',
          fieldErrors: []
        }
      }
    })
    const { onOpenChange } = renderDialog()
    await userEvent.type(screen.getByLabelText(/Họ và tên/), ' mới')

    await userEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Bạn không có quyền cập nhật hồ sơ.')
    expect(onOpenChange).not.toHaveBeenCalledWith(false)
  })

  it('disables profile actions while an avatar upload is pending', async () => {
    let resolveUpload: (() => void) | undefined
    fileApiMock.upload.mockReturnValue(
      new Promise((resolve) => {
        resolveUpload = () =>
          resolve({
            data: {
              content: {
                id: 'f1',
                originalName: 'avatar.png',
                contentType: 'image/png',
                size: 3,
                downloadUrl: '/api/files/f1'
              }
            }
          })
      })
    )
    renderDialog()
    const input = document.querySelector<HTMLInputElement>('input[type="file"]')
    if (!input) throw new Error('Expected avatar file input')

    await userEvent.upload(input, new File(['png'], 'avatar.png', { type: 'image/png' }))

    expect(screen.getByRole('button', { name: 'Lưu thay đổi' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Hủy' })).toBeDisabled()
    resolveUpload?.()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Lưu thay đổi' })).toBeEnabled())
  })
})
