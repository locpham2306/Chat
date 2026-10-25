import { useQuery } from '@tanstack/react-query'
import userApi, { type SearchUsersParams } from '~/apis/user.api'
import type { Page } from '~/types/pagination.type'
import type { User } from '~/types/user.type'

export const userSearchKeys = {
  all: ['users', 'search'] as const,
  list: (keyword: string, page: number, size: number) => ['users', 'search', { keyword, page, size }] as const
}

export const fetchUsers = async ({ keyword = '', page = 0, size = 20, signal }: SearchUsersParams = {}) => {
  const response = await userApi.search({ keyword, page, size, signal })
  const result = response.data.content

  return {
    items: result.data,
    page,
    size,
    totalPages: result.totalPages,
    totalElements: result.totalElements
  } satisfies Page<User>
}

export const scheduleDebouncedValue = <T>(value: T, delay: number, onValue: (value: T) => void) => {
  const timeoutId = globalThis.setTimeout(() => onValue(value), delay)
  return () => globalThis.clearTimeout(timeoutId)
}

export const createSearchParams = (keyword: string) => ({
  keyword: keyword.trim(),
  page: 0
})

export const clampSearchPage = (currentPage: number, totalPages: number) =>
  Math.min(currentPage, Math.max(0, totalPages - 1))

export const useUserSearchQuery = (keyword: string, page: number, size = 20) => {
  const normalizedKeyword = keyword.trim()

  return useQuery({
    queryKey: userSearchKeys.list(normalizedKeyword, page, size),
    queryFn: ({ signal }) => fetchUsers({ keyword: normalizedKeyword, page, size, signal })
  })
}
