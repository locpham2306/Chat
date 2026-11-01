import { useEffect, useState } from 'react'
import { AlertCircle, Check, ChevronLeft, ChevronRight, LoaderCircle, Search, UserPlus, X } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from '~/components/ui/avatar'
import { Badge } from '~/components/ui/badge'
import { Button } from '~/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from '~/components/ui/dialog'
import { Input } from '~/components/ui/input'
import { ScrollArea } from '~/components/ui/scroll-area'
import { useAddGroupMembersMutation, useAvailableMembersQuery } from '~/hooks/use-group-members'
import { clampSearchPage, createSearchParams, scheduleDebouncedValue } from '~/hooks/use-user-search'
import { getApiErrorMessage } from '~/lib/api-error'
import { cn } from '~/lib/utils'
import type { User } from '~/types/user.type'

interface AddMembersDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  conversationId: string
  onSuccess?: () => void
}

const PAGE_SIZE = 20

export const AddMembersDialog = ({ open, onOpenChange, conversationId, onSuccess }: AddMembersDialogProps) => {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {open && (
        <AddMembersDialogContent onOpenChange={onOpenChange} conversationId={conversationId} onSuccess={onSuccess} />
      )}
    </Dialog>
  )
}

interface AddMembersDialogContentProps {
  onOpenChange: (open: boolean) => void
  conversationId: string
  onSuccess?: () => void
}

const AddMembersDialogContent = ({ onOpenChange, conversationId, onSuccess }: AddMembersDialogContentProps) => {
  const addMembersMutation = useAddGroupMembersMutation(conversationId)
  const [selectedUsers, setSelectedUsers] = useState<User[]>([])
  const [keyword, setKeyword] = useState('')
  const [searchParams, setSearchParams] = useState(() => createSearchParams(''))

  // Debounce search input
  useEffect(
    () => scheduleDebouncedValue(keyword, 300, (nextKeyword) => setSearchParams(createSearchParams(nextKeyword))),
    [keyword]
  )

  const availableQuery = useAvailableMembersQuery(conversationId, {
    keyword: searchParams.keyword,
    page: searchParams.page,
    size: PAGE_SIZE
  })

  // Clamp page khi tổng trang thay đổi
  useEffect(() => {
    if (!availableQuery.data) return
    const nextPage = clampSearchPage(searchParams.page, availableQuery.data.totalPages)
    if (nextPage !== searchParams.page) {
      // oxlint-disable-next-line react/set-state-in-effect
      setSearchParams((current) => ({ ...current, page: nextPage }))
    }
  }, [searchParams.page, availableQuery.data])

  const totalPages = availableQuery.data?.totalPages ?? 0
  const isDebouncing = keyword.trim() !== searchParams.keyword
  const isLoading = isDebouncing || availableQuery.isPending
  const availableUsers = availableQuery.data?.data ?? []

  const isSelected = (userId: string) => selectedUsers.some((u) => u.id === userId)

  const handleToggleUser = (user: User) => {
    setSelectedUsers((prev) => {
      if (prev.some((u) => u.id === user.id)) {
        return prev.filter((u) => u.id !== user.id)
      }
      return [...prev, user]
    })
  }

  const handleRemoveSelected = (userId: string) => {
    setSelectedUsers((prev) => prev.filter((u) => u.id !== userId))
  }

  const handleAddMembers = async () => {
    if (selectedUsers.length === 0) return

    try {
      await addMembersMutation.mutateAsync(selectedUsers.map((u) => u.id))
      onOpenChange(false)
      onSuccess?.()
    } catch {
      // Error handled by mutation
    }
  }

  return (
    <DialogContent className='flex h-[calc(100vh-4rem)] max-h-[38rem] w-[calc(100%_-_2rem)] max-w-lg flex-col gap-0 overflow-hidden p-0 sm:rounded-xl'>
      <DialogHeader className='shrink-0 border-b p-5 pr-12'>
        <div className='flex items-center gap-2'>
          <div className='flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary'>
            <UserPlus className='h-4 w-4' />
          </div>
          <DialogTitle>Thêm thành viên vào nhóm</DialogTitle>
        </div>
        <DialogDescription>Tìm kiếm và chọn người dùng để mời tham gia nhóm chat này.</DialogDescription>
      </DialogHeader>

      <div className='flex flex-1 flex-col overflow-hidden'>
        {/* Danh sách người dùng đã chọn */}
        {selectedUsers.length > 0 && (
          <div className='shrink-0 border-b bg-muted/30 p-3'>
            <div className='flex items-center justify-between pb-1.5'>
              <span className='text-xs font-semibold text-muted-foreground'>Đã chọn ({selectedUsers.length}):</span>
              <button
                type='button'
                onClick={() => setSelectedUsers([])}
                className='text-[11px] text-muted-foreground hover:text-foreground hover:underline'
              >
                Bỏ chọn tất cả
              </button>
            </div>
            <div className='flex flex-wrap gap-1.5 max-h-24 overflow-y-auto'>
              {selectedUsers.map((user) => (
                <Badge key={user.id} variant='secondary' className='gap-1 pr-1 pl-2 py-0.5 text-xs font-normal'>
                  <span className='truncate max-w-[120px]'>{user.fullName}</span>
                  <button
                    type='button'
                    onClick={() => handleRemoveSelected(user.id)}
                    className='rounded-full p-0.5 hover:bg-muted text-muted-foreground hover:text-foreground'
                    aria-label={`Bỏ chọn ${user.fullName}`}
                  >
                    <X className='h-3 w-3' />
                  </button>
                </Badge>
              ))}
            </div>
          </div>
        )}

        {/* Ô tìm kiếm */}
        <div className='relative shrink-0 border-b p-3'>
          <Search className='absolute top-1/2 left-6 h-4 w-4 -translate-y-1/2 text-muted-foreground' />
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder='Tìm theo họ tên hoặc username...'
            className='pl-9 text-sm'
            autoFocus
          />
        </div>

        {/* Danh sách ứng viên */}
        <ScrollArea className='flex-1'>
          <div className='p-3'>
            {isLoading ? (
              <div className='flex items-center justify-center py-12 text-sm text-muted-foreground'>
                <LoaderCircle className='mr-2 h-4 w-4 animate-spin' />
                <span>Đang tìm kiếm...</span>
              </div>
            ) : availableQuery.isError ? (
              <div className='flex flex-col items-center justify-center py-10 text-center'>
                <p className='text-xs text-destructive'>{getApiErrorMessage(availableQuery.error)}</p>
                <Button variant='ghost' size='sm' onClick={() => availableQuery.refetch()} className='mt-2 text-xs'>
                  Thử lại
                </Button>
              </div>
            ) : availableUsers.length === 0 ? (
              <div className='flex flex-col items-center justify-center py-12 text-center text-muted-foreground'>
                <p className='text-sm font-medium'>Không tìm thấy người dùng phù hợp</p>
                <p className='mt-1 text-xs'>
                  {keyword ? 'Thử tìm với từ khóa khác' : 'Tất cả người dùng khả dụng đã có trong nhóm'}
                </p>
              </div>
            ) : (
              <div className='space-y-1'>
                {availableUsers.map((user) => {
                  const selected = isSelected(user.id)
                  const initials = (user.fullName.trim().slice(0, 1) || 'U').toUpperCase()
                  const avatarUrl = user.avatarFileId ? `/api/files/${user.avatarFileId}` : null

                  return (
                    <div
                      key={user.id}
                      role='button'
                      tabIndex={0}
                      onClick={() => handleToggleUser(user)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          handleToggleUser(user)
                        }
                      }}
                      className={cn(
                        'flex items-center justify-between p-2.5 rounded-lg cursor-pointer transition-colors text-left border select-none',
                        selected
                          ? 'bg-primary/10 border-primary/30 text-foreground'
                          : 'border-transparent hover:bg-muted/60 text-foreground'
                      )}
                    >
                      <div className='flex items-center gap-3 min-w-0'>
                        <div className='relative shrink-0'>
                          <Avatar className='h-9 w-9'>
                            {avatarUrl && <AvatarImage src={avatarUrl} alt={user.fullName} />}
                            <AvatarFallback className='bg-primary/10 text-primary font-semibold text-xs'>
                              {initials}
                            </AvatarFallback>
                          </Avatar>
                          {user.status === 'ONLINE' && (
                            <span
                              className='absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full bg-emerald-500 ring-2 ring-background'
                              title='Đang trực tuyến'
                            />
                          )}
                        </div>

                        <div className='min-w-0'>
                          <p className='text-xs font-semibold truncate leading-tight'>{user.fullName}</p>
                          <p className='text-[11px] text-muted-foreground truncate leading-tight mt-0.5'>
                            @{user.username}
                          </p>
                        </div>
                      </div>

                      <div
                        className={cn(
                          'h-5 w-5 rounded-md border flex items-center justify-center shrink-0 transition-colors',
                          selected ? 'bg-primary border-primary text-primary-foreground' : 'border-muted-foreground/30'
                        )}
                        aria-hidden='true'
                      >
                        {selected && <Check className='h-3.5 w-3.5' />}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </ScrollArea>

        {/* Phân trang */}
        {totalPages > 1 && (
          <div className='flex items-center justify-between border-t p-2 px-4 text-xs text-muted-foreground shrink-0 bg-muted/10'>
            <span>
              Trang {searchParams.page + 1} / {totalPages}
            </span>
            <div className='flex items-center gap-1'>
              <Button
                variant='ghost'
                size='icon'
                className='h-7 w-7'
                disabled={searchParams.page === 0 || isLoading}
                onClick={() => setSearchParams((prev) => ({ ...prev, page: prev.page - 1 }))}
                aria-label='Trang trước'
              >
                <ChevronLeft className='h-3.5 w-3.5' />
              </Button>
              <Button
                variant='ghost'
                size='icon'
                className='h-7 w-7'
                disabled={searchParams.page + 1 >= totalPages || isLoading}
                onClick={() => setSearchParams((prev) => ({ ...prev, page: prev.page + 1 }))}
                aria-label='Trang tiếp'
              >
                <ChevronRight className='h-3.5 w-3.5' />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <DialogFooter className='shrink-0 border-t p-4 flex flex-col sm:flex-row items-center justify-between gap-2 bg-muted/10'>
        {addMembersMutation.error ? (
          <div className='flex items-center gap-1.5 text-xs text-destructive max-w-xs truncate'>
            <AlertCircle className='h-3.5 w-3.5 shrink-0' />
            <span>{getApiErrorMessage(addMembersMutation.error)}</span>
          </div>
        ) : (
          <span className='text-xs text-muted-foreground'>
            {selectedUsers.length > 0 ? `Sẽ thêm ${selectedUsers.length} người vào nhóm` : 'Chọn ít nhất 1 người dùng'}
          </span>
        )}

        <div className='flex items-center gap-2 w-full sm:w-auto justify-end'>
          <Button
            type='button'
            variant='outline'
            size='sm'
            onClick={() => onOpenChange(false)}
            disabled={addMembersMutation.isPending}
          >
            Hủy
          </Button>

          <Button
            type='button'
            size='sm'
            onClick={handleAddMembers}
            disabled={selectedUsers.length === 0 || addMembersMutation.isPending}
            className='gap-1.5'
          >
            {addMembersMutation.isPending && <LoaderCircle className='h-3.5 w-3.5 animate-spin' />}
            Thêm vào nhóm
          </Button>
        </div>
      </DialogFooter>
    </DialogContent>
  )
}
