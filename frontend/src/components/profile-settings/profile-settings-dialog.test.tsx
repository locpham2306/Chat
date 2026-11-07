import { describe, expect, it } from 'vitest'
import { canChangeProfileDialogOpenState } from '~/components/profile-settings/profile-settings.utils'

describe('canChangeProfileDialogOpenState', () => {
  it('không cho đóng hộp thoại khi ảnh đại diện đang tải lên', () => {
    expect(canChangeProfileDialogOpenState(false, true)).toBe(false)
  })

  it('cho phép đóng hộp thoại sau khi upload hoàn tất', () => {
    expect(canChangeProfileDialogOpenState(false, false)).toBe(true)
  })
})
