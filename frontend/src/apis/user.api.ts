import http from '~/lib/http'
import type { ApiPage, ApiResponse } from '~/types/api-response.type'
import type { UpdateProfileBody, User } from '~/types/user.type'

export interface SearchUsersParams {
  keyword?: string
  page?: number
  size?: number
  signal?: AbortSignal
}

const userApi = {
  updateProfile: (body: UpdateProfileBody) => http.put<ApiResponse<User>>('/api/users/me', body),
  search: ({ keyword = '', page = 0, size = 20, signal }: SearchUsersParams = {}) =>
    http.get<ApiResponse<ApiPage<User[]>>>('/api/users', {
      params: { keyword: keyword.trim(), page, size },
      signal
    })
}

export default userApi
