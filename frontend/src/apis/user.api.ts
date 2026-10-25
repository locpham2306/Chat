import http from '~/lib/http'
import type { ApiPage, ApiResponse } from '~/types/api-response.type'
import type { User } from '~/types/user.type'

export interface SearchUsersParams {
  keyword?: string
  page?: number
  size?: number
  signal?: AbortSignal
}

const userApi = {
  search: ({ keyword = '', page = 0, size = 20, signal }: SearchUsersParams = {}) =>
    http.get<ApiResponse<ApiPage<User[]>>>('/api/users', {
      params: { keyword: keyword.trim(), page, size },
      signal
    })
}

export default userApi
