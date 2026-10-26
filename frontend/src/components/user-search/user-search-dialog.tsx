import { useEffect, useState } from 'react'
import { AlertCircle, ChevronLeft, ChevronRight, LoaderCircle, Search, UserRoundSearch } from 'lucide-react'
import { Avatar, AvatarFallback } from '~/components/ui/avatar'
import { Badge } from '~/components/ui/badge'
import { Button } from '~/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '~/components/ui/dialog'
import { Input } from '~/components/ui/input'
import { ScrollArea } from '~/components/ui/scroll-area'
import {
  clampSearchPage,
  createSearchParams,
  scheduleDebouncedValue,
  useUserSearchQuery
} from '~/hooks/use-user-search'
import { getApiErrorMessage } from '~/lib/api-error'
import type { User } from '~/types/user.type'

interface UserSearchDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSelectUser: (user: User) => void
  selectingUserId?: string
  selectionError?: string
}

const PAGE_SIZE = 20

export const UserSearchDialog = ({
  open,
  onOpenChange,
  onSelectUser,
  selectingUserId,
  selectionError
}: UserSearchDialogProps) => {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {open && (
        <UserSearchDialogContent
          onSelectUser={onSelectUser}
          selectingUserId={selectingUserId}
          selectionError={selectionError}
        />
      )}
    </Dialog>
  )
}

type UserSearchDialogContentProps = Pick<UserSearchDialogProps, 'onSelectUser' | 'selectingUserId' | 'selectionError'>

const UserSearchDialogContent = ({ onSelectUser, selectingUserId, selectionError }: UserSearchDialogContentProps) => {
  const [keyword, setKeyword] = useState('')
  const [searchParams, setSearchParams] = useState(() => createSearchParams(''))
  const searchQuery = useUserSearchQuery(searchParams.keyword, searchParams.page, PAGE_SIZE)

  const users = searchQuery.data?.data ?? []
  const totalPages = searchQuery.data?.totalPages ?? 0
  const totalElements = searchQuery.data?.totalElements ?? 0
  const isDebouncing = keyword.trim() !== searchParams.keyword
  const isLoading = isDebouncing || searchQuery.isPending

  useEffect(
    () => scheduleDebouncedValue(keyword, 350, (nextKeyword) => setSearchParams(createSearchParams(nextKeyword))),
    [keyword]
  )

  useEffect(() => {
    if (!searchQuery.data) return

    const nextPage = clampSearchPage(searchParams.page, searchQuery.data.totalPages)
    if (nextPage !== searchParams.page) {
      // The server can shrink the result set while this page is open.
      // oxlint-disable-next-line react/set-state-in-effect
      setSearchParams((current) => ({ ...current, page: nextPage }))
    }
  }, [searchParams.page, searchQuery.data])

  return (
    <DialogContent className='flex h-[calc(100vh-2rem)] max-h-[36rem] w-[calc(100%_-_2rem)] max-w-xl flex-col gap-0 overflow-hidden p-0 sm:rounded-xl'>
      <DialogHeader className='shrink-0 border-b p-5 pr-12'>
        <DialogTitle>Tìm người dùng</DialogTitle>
        <DialogDescription>Tìm theo tên tài khoản hoặc họ và tên để bắt đầu trò chuyện.</DialogDescription>
      </DialogHeader>

      <div className='shrink-0 border-b p-4'>
        <div className='relative'>
          <Search
            className='pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground'
            aria-hidden='true'
          />
          <Input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder='Nhập tên hoặc tài khoản...'
            aria-label='Tìm kiếm người dùng'
            className='pl-9 pr-9'
            autoFocus
          />
          {searchQuery.isFetching && !isLoading && (
            <LoaderCircle
              className='absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground'
              aria-label='Đang cập nhật kết quả'
            />
          )}
        </div>
        <p className='mt-2 text-xs text-muted-foreground' aria-live='polite'>
          {isLoading ? 'Đang tìm kiếm...' : `${totalElements} người dùng`}
        </p>
      </div>

      {selectionError && (
        <div role='alert' className='shrink-0 border-b bg-destructive/10 px-4 py-3 text-sm text-destructive'>
          {selectionError}
        </div>
      )}

      <div className='min-h-0 flex-1 overflow-hidden'>
        {isLoading ? (
          <div role='status' className='flex h-full items-center justify-center gap-2 text-sm text-muted-foreground'>
            <LoaderCircle className='h-4 w-4 animate-spin' aria-hidden='true' />
            Đang tải người dùng...
          </div>
        ) : searchQuery.isError ? (
          <div role='alert' className='flex h-full flex-col items-center justify-center gap-3 p-6 text-center'>
            <AlertCircle className='h-8 w-8 text-destructive' aria-hidden='true' />
            <div>
              <p className='text-sm font-medium'>Không thể tìm người dùng</p>
              <p className='mt-1 text-xs text-muted-foreground'>{getApiErrorMessage(searchQuery.error)}</p>
            </div>
            <Button variant='outline' size='sm' onClick={() => searchQuery.refetch()}>
              Thử lại
            </Button>
          </div>
        ) : users.length === 0 ? (
          <div role='status' className='flex h-full flex-col items-center justify-center gap-3 p-6 text-center'>
            <UserRoundSearch className='h-9 w-9 text-muted-foreground' aria-hidden='true' />
            <div>
              <p className='text-sm font-medium'>Không tìm thấy người dùng</p>
              <p className='mt-1 text-xs text-muted-foreground'>Thử một tên hoặc tài khoản khác.</p>
            </div>
          </div>
        ) : (
          <ScrollArea className='h-full'>
            <div className='space-y-1 p-2'>
              {users.map((user) => (
                <button
                  key={user.id}
                  type='button'
                  onClick={() => onSelectUser(user)}
                  disabled={Boolean(selectingUserId)}
                  aria-busy={selectingUserId === user.id}
                  className='flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'
                >
                  <Avatar className='h-10 w-10 shrink-0'>
                    <AvatarFallback>{(user.fullName || user.username).slice(0, 1).toUpperCase()}</AvatarFallback>
                  </Avatar>
                  <div className='min-w-0 flex-1'>
                    <p className='truncate text-sm font-medium'>{user.fullName || user.username}</p>
                    <p className='truncate text-xs text-muted-foreground'>@{user.username}</p>
                  </div>
                  {selectingUserId === user.id ? (
                    <LoaderCircle
                      className='h-4 w-4 animate-spin text-muted-foreground'
                      aria-label='Đang mở hội thoại'
                    />
                  ) : (
                    <Badge variant={user.status === 'ONLINE' ? 'online' : 'offline'}>
                      {user.status === 'ONLINE' ? 'Online' : 'Offline'}
                    </Badge>
                  )}
                </button>
              ))}
            </div>
          </ScrollArea>
        )}
      </div>

      <div className='flex min-h-14 shrink-0 items-center justify-between border-t px-4 py-3 text-xs text-muted-foreground'>
        <Button
          variant='ghost'
          size='sm'
          disabled={searchParams.page === 0 || isLoading}
          onClick={() => setSearchParams((current) => ({ ...current, page: Math.max(0, current.page - 1) }))}
        >
          <ChevronLeft aria-hidden='true' />
          Trước
        </Button>
        <span aria-live='polite'>{totalPages > 0 ? `${searchParams.page + 1} / ${totalPages}` : '0 / 0'}</span>
        <Button
          variant='ghost'
          size='sm'
          disabled={searchParams.page + 1 >= totalPages || isLoading}
          onClick={() => setSearchParams((current) => ({ ...current, page: current.page + 1 }))}
        >
          Sau
          <ChevronRight aria-hidden='true' />
        </Button>
      </div>
    </DialogContent>
  )
}
