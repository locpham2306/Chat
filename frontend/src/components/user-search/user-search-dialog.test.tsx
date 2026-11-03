import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { UserSearchDialog } from './user-search-dialog'

const { searchQueryMock } = vi.hoisted(() => ({ searchQueryMock: vi.fn() }))

vi.mock('~/hooks/use-user-search', async (importOriginal) => ({
  ...(await importOriginal<typeof import('~/hooks/use-user-search')>()),
  useUserSearchQuery: searchQueryMock
}))

describe('UserSearchDialog accessibility', () => {
  it('connects the search field with a named result list and announces the result count', () => {
    searchQueryMock.mockReturnValue({
      data: {
        data: [
          {
            id: 'u2',
            username: 'an',
            fullName: 'Nguyễn Văn An',
            avatarFileId: null,
            status: 'ONLINE',
            lastSeen: null
          }
        ],
        totalPages: 1,
        totalElements: 1
      },
      isPending: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: vi.fn()
    })

    render(<UserSearchDialog open onOpenChange={vi.fn()} onSelectUser={vi.fn()} />)

    const search = screen.getByRole('textbox', { name: 'Tìm kiếm người dùng' })
    const results = screen.getByRole('region', { name: 'Kết quả tìm kiếm người dùng' })
    expect(screen.getByRole('list', { name: 'Kết quả tìm kiếm người dùng' })).toBeInTheDocument()
    expect(search).toHaveAttribute('aria-controls', results.id)
    expect(search).toHaveAttribute('aria-describedby', 'user-search-status')
    expect(screen.getByRole('status')).toHaveTextContent('1 người dùng')
  })

  it.each([
    {
      name: 'loading',
      query: { data: undefined, isPending: true, isFetching: true, isError: false, error: null, refetch: vi.fn() }
    },
    {
      name: 'error',
      query: {
        data: undefined,
        isPending: false,
        isFetching: false,
        isError: true,
        error: new Error('Mất kết nối'),
        refetch: vi.fn()
      }
    },
    {
      name: 'empty',
      query: {
        data: { data: [], totalPages: 0, totalElements: 0 },
        isPending: false,
        isFetching: false,
        isError: false,
        error: null,
        refetch: vi.fn()
      }
    }
  ])('keeps the controlled result region mounted while $name', ({ query }) => {
    searchQueryMock.mockReturnValue(query)

    render(<UserSearchDialog open onOpenChange={vi.fn()} onSelectUser={vi.fn()} />)

    const search = screen.getByRole('textbox', { name: 'Tìm kiếm người dùng' })
    const controlledId = search.getAttribute('aria-controls')
    expect(controlledId).toBeTruthy()
    expect(document.getElementById(controlledId!)).toHaveAccessibleName('Kết quả tìm kiếm người dùng')
  })
})
