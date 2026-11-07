import type { AxiosResponse } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { clampSearchPage, createSearchParams, fetchUsers, scheduleDebouncedValue } from '~/hooks/use-user-search'
import http from '~/lib/http'

const originalAdapter = http.defaults.adapter

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

describe('fetchUsers', () => {
  it('giữ ApiPage trong content của HTTP-07 cho giao diện', async () => {
    http.defaults.adapter = async (config) =>
      ({
        data: {
          code: '0',
          strCode: 'success',
          message: 'SUCCESSFUL',
          content: {
            data: [
              {
                id: 'user-1',
                username: 'an',
                fullName: 'Nguyễn Văn An',
                avatarFileId: null,
                status: 'OFFLINE',
                lastSeen: null
              }
            ],
            totalPages: 3,
            totalElements: 41
          }
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config
      }) satisfies AxiosResponse

    const page = await fetchUsers({ keyword: 'an', page: 1, size: 20 })

    expect(page).toEqual({
      data: [
        {
          id: 'user-1',
          username: 'an',
          fullName: 'Nguyễn Văn An',
          avatarFileId: null,
          status: 'OFFLINE',
          lastSeen: null
        }
      ],
      totalPages: 3,
      totalElements: 41
    })
  })
})

describe('scheduleDebouncedValue', () => {
  it('chỉ cập nhật giá trị sau khoảng debounce', () => {
    vi.useFakeTimers()
    const onValue = vi.fn()

    const cancel = scheduleDebouncedValue('an', 350, onValue)

    vi.advanceTimersByTime(349)
    expect(onValue).not.toHaveBeenCalled()
    vi.advanceTimersByTime(1)
    expect(onValue).toHaveBeenCalledOnce()
    expect(onValue).toHaveBeenCalledWith('an')

    cancel()
    vi.useRealTimers()
  })

  it('hủy cập nhật của keyword cũ', () => {
    vi.useFakeTimers()
    const onValue = vi.fn()
    const cancel = scheduleDebouncedValue('a', 350, onValue)

    cancel()
    vi.advanceTimersByTime(350)

    expect(onValue).not.toHaveBeenCalled()
    vi.useRealTimers()
  })
})

describe('search params', () => {
  it('chuẩn hóa keyword và quay về trang đầu trong cùng một lần cập nhật', () => {
    expect(createSearchParams('  an  ')).toEqual({ keyword: 'an', page: 0 })
  })

  it.each([
    { currentPage: 2, totalPages: 1, expectedPage: 0 },
    { currentPage: 1, totalPages: 3, expectedPage: 1 },
    { currentPage: 1, totalPages: 0, expectedPage: 0 }
  ])(
    'giới hạn trang $currentPage về $expectedPage khi có $totalPages trang',
    ({ currentPage, totalPages, expectedPage }) => {
      expect(clampSearchPage(currentPage, totalPages)).toBe(expectedPage)
    }
  )
})
