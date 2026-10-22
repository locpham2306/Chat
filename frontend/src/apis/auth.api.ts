import http from '~/lib/http'
import type { LoginBody, LoginResponse, RegisterBody } from '~/types/auth.type'
import type { User } from '~/types/user.type'

const authApi = {
  login: (body: LoginBody) => http.post<LoginResponse>('/api/auth/login', body),
  register: (body: RegisterBody) => http.post<User>('/api/auth/register', body),
  getProfile: () => http.get<User>('/api/users/me')
}

export default authApi
