import { describe, expect, it } from 'vitest'
import { loginSchema, registerSchema } from './auth.type'

describe('auth schemas', () => {
  it('rejects empty login fields with the messages shown by the form', () => {
    const result = loginSchema.safeParse({ username: '   ', password: '' })

    expect(result.success).toBe(false)
    if (result.success) return
    expect(result.error.flatten().fieldErrors).toEqual({
      username: ['Vui lòng nhập tài khoản'],
      password: ['Vui lòng nhập mật khẩu']
    })
  })

  it('trims register text fields while preserving a valid password', () => {
    const result = registerSchema.parse({
      fullName: '  Nguyễn Văn A  ',
      username: '  nguyenvana  ',
      password: 'secret'
    })

    expect(result).toEqual({ fullName: 'Nguyễn Văn A', username: 'nguyenvana', password: 'secret' })
  })
})
