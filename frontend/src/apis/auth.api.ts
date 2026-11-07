import http from '~/lib/http'
import type { ApiResponse } from '~/types/api-response.type'
import type { LoginBody, LoginResponse, RegisterBody } from '~/types/auth.type'
import type { User } from '~/types/user.type'

const authApi = {
  login: (body: LoginBody) => http.post<ApiResponse<LoginResponse>>('/api/auth/login', body),
  register: (body: RegisterBody) => http.post<ApiResponse<User>>('/api/auth/register', body),
  logout: () => http.post<void>('/api/auth/logout'),
  getProfile: () => http.get<ApiResponse<User>>('/api/users/me')
}

export default authApi
