import { z } from 'zod'

const requiredText = (message: string) => z.string().trim().min(1, message)

export const loginSchema = z.object({
  username: requiredText('Vui lòng nhập tài khoản'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu')
})

export const registerSchema = loginSchema.extend({
  fullName: requiredText('Vui lòng nhập họ và tên')
})

export type LoginBody = z.infer<typeof loginSchema>
export type RegisterBody = z.infer<typeof registerSchema>

export interface LoginResponse {
  message: string
}
