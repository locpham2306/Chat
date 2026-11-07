import { describe, expect, it, vi } from 'vitest'

const put = vi.hoisted(() => vi.fn())

vi.mock('~/lib/http', () => ({
  default: { put }
}))

import userApi from '~/apis/user.api'

describe('userApi.updateProfile', () => {
  it('gửi đủ họ tên và avatarFileId null để xóa avatar', async () => {
    put.mockResolvedValue({ data: { content: {} } })

    await userApi.updateProfile({ fullName: 'Nguyễn Văn An', avatarFileId: null })

    expect(put).toHaveBeenCalledWith('/api/users/me', {
      fullName: 'Nguyễn Văn An',
      avatarFileId: null
    })
  })
})
