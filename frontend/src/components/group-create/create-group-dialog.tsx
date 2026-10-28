import { useEffect, useState } from 'react'
import { AlertCircle, Check, ChevronLeft, ChevronRight, LoaderCircle, Search, Users, X } from 'lucide-react'
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
import { useCreateGroupMutation } from '~/hooks/use-conversation'
import {
  clampSearchPage,
  createSearchParams,
  scheduleDebouncedValue,
  useUserSearchQuery
} from '~/hooks/use-user-search'
import { getApiErrorMessage } from '~/lib/api-error'
import { cn } from '~/lib/utils'
import { useAuthStore } from '~/stores/use-auth-store'
import type { GroupConversation } from '~/types/conversation.type'
import type { User } from '~/types/user.type'

interface CreateGroupDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess?: (newGroup: GroupConversation) => void
}

const SEARCH_PAGE_SIZE = 20

export const CreateGroupDialog = ({ open, onOpenChange, onSuccess }: CreateGroupDialogProps) => {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {open && <CreateGroupDialogContent onOpenChange={onOpenChange} onSuccess={onSuccess} />}
    </Dialog>
  )
}

interface CreateGroupDialogContentProps {
  onOpenChange: (open: boolean) => void
  onSuccess?: (newGroup: GroupConversation) => void
}

const CreateGroupDialogContent = ({ onOpenChange, onSuccess }: CreateGroupDialogContentProps) => {
  const currentUser = useAuthStore((state) => state.user)
  const createGroupMutation = useCreateGroupMutation()

  const [groupName, setGroupName] = useState('')
  const [nameTouched, setNameTouched] = useState(false)
  const [selectedUsers, setSelectedUsers] = useState<User[]>([])

  const [keyword, setKeyword] = useState('')
  const [searchParams, setSearchParams] = useState(() => createSearchParams(''))

  const searchQuery = useUserSearchQuery(searchParams.keyword, searchParams.page, SEARCH_PAGE_SIZE)

  const totalPages = searchQuery.data?.totalPages ?? 0
  const isDebouncing = keyword.trim() !== searchParams.keyword
  const isLoading = isDebouncing || searchQuery.isPending

  // Debounce search input
  useEffect(
    () => scheduleDebouncedValue(keyword, 300, (nextKeyword) => setSearchParams(createSearchParams(nextKeyword))),
    [keyword]
  )

  // Clamp page khi kết quả thay đổi
  useEffect(() => {
    if (!searchQuery.data) return
    const nextPage = clampSearchPage(searchParams.page, searchQuery.data.totalPages)
    if (nextPage !== searchParams.page) {
      // oxlint-disable-next-line react/set-state-in-effect
      setSearchParams((current) => ({ ...current, page: nextPage }))
    }
  }, [searchParams.page, searchQuery.data])

  // Danh sách người dùng kết quả, tự động lọc bỏ người dùng hiện tại
  const availableUsers = (searchQuery.data?.data ?? []).filter((user) => user.id !== currentUser?.id)

  const isSelected = (userId: string) => selectedUsers.some((u) => u.id === userId)

  const handleToggleUser = (user: User) => {
    setSelectedUsers((prev) => {
      if (prev.some((u) => u.id === user.id)) {
        return prev.filter((u) => u.id !== user.id)
      }
      return [...prev, user]
    })
  }

  const handleRemoveUser = (userId: string) => {
    setSelectedUsers((prev) => prev.filter((u) => u.id !== userId))
  }

  const trimmedName = groupName.trim()
  const nameError = nameTouched && !trimmedName ? 'Vui lòng nhập tên nhóm' : ''

  const handleCreate = async () => {
    setNameTouched(true)
    if (!trimmedName || selectedUsers.length === 0) return

    try {
      const newGroup = await createGroupMutation.mutateAsync({
        name: trimmedName,
        memberIds: selectedUsers.map((u) => u.id)
      })

      onOpenChange(false)
      onSuccess?.(newGroup)
    } catch {
      // Mutation error will be displayed in the UI
    }
  }

  return (
    <DialogContent className='flex h-[calc(100vh-2rem)] max-h-[38rem] w-[calc(100%_-_2rem)] max-w-lg flex-col gap-0 overflow-hidden p-0 sm:rounded-xl'>
      <DialogHeader className='shrink-0 border-b p-5 pr-12'>
        <div className='flex items-center gap-2'>
          <div className='flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary'>
            <Users className='h-4 w-4' />
          </div>
          <DialogTitle>Tạo nhóm chat mới</DialogTitle>
        </div>
        <DialogDescription>Đặt tên cho nhóm và chọn ít nhất 1 thành viên để bắt đầu cuộc trò chuyện.</DialogDescription>
      </DialogHeader>

      <div className='flex flex-1 flex-col overflow-hidden'>
        {/* Tên nhóm */}
        <div className='shrink-0 border-b p-4 space-y-1.5'>
          <label
            htmlFor='group-name'
            className='text-xs font-semibold text-foreground flex items-center justify-between'
          >
            <span>
              Tên nhóm <span className='text-destructive'>*</span>
            </span>
            <span className='text-[10px] text-muted-foreground font-normal'>{groupName.length}/100</span>
          </label>
          <Input
            id='group-name'
            value={groupName}
            maxLength={100}
            onChange={(e) => {
              setGroupName(e.target.value)
              if (!nameTouched) setNameTouched(true)
            }}
            placeholder='Ví dụ: Nhóm học tập LTM, Dự án Chat...'
            autoFocus
          />
          {nameError && <p className='text-xs text-destructive'>{nameError}</p>}
        </div>

        {/* Danh sách thành viên đã chọn */}
        {selectedUsers.length > 0 && (
          <div className='shrink-0 border-b bg-muted/30 p-3'>
            <div className='flex items-center justify-between mb-2'>
              <span className='text-xs font-medium text-muted-foreground'>Đã chọn ({selectedUsers.length})</span>
              <button
                type='button'
                onClick={() => setSelectedUsers([])}
                className='text-[11px] text-muted-foreground hover:text-foreground transition-colors'
              >
                Xóa tất cả
              </button>
            </div>
            <div className='flex flex-wrap gap-1.5 max-h-24 overflow-y-auto'>
              {selectedUsers.map((user) => (
                <Badge
                  key={user.id}
                  variant='secondary'
                  className='flex items-center gap-1.5 py-1 pl-1.5 pr-2 text-xs font-normal bg-background border shadow-xs'
                >
                  <Avatar className='h-4 w-4'>
                    {user.avatarFileId && <AvatarImage src={`/api/files/${user.avatarFileId}`} alt={user.fullName} />}
                    <AvatarFallback className='text-[9px]'>{user.fullName.slice(0, 1).toUpperCase()}</AvatarFallback>
                  </Avatar>
                  <span className='max-w-[120px] truncate'>{user.fullName}</span>
                  <button
                    type='button'
                    onClick={() => handleRemoveUser(user.id)}
                    className='ml-0.5 rounded-full p-0.5 hover:bg-muted text-muted-foreground hover:text-foreground'
                    aria-label={`Xóa ${user.fullName}`}
                  >
                    <X className='h-3 w-3' />
                  </button>
                </Badge>
              ))}
            </div>
          </div>
        )}

        {/* Vùng tìm kiếm thành viên */}
        <div className='shrink-0 border-b p-3'>
          <div className='relative'>
            <Search className='pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground' />
            <Input
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value)
                setSearchParams((prev) => ({ ...prev, page: 0 }))
              }}
              placeholder='Tìm người theo tên hoặc username...'
              className='pl-9 pr-9 text-xs'
            />
            {isLoading && (
              <LoaderCircle className='absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground' />
            )}
          </div>
        </div>

        {/* Danh sách người dùng để chọn */}
        <ScrollArea className='flex-1'>
          <div className='p-2 space-y-1'>
            {isLoading ? (
              <div className='flex items-center justify-center p-8 text-xs text-muted-foreground gap-2'>
                <LoaderCircle className='h-4 w-4 animate-spin' />
                Đang tìm kiếm...
              </div>
            ) : availableUsers.length === 0 ? (
              <div className='p-8 text-center text-xs text-muted-foreground'>
                {keyword.trim()
                  ? `Không tìm thấy người dùng nào phù hợp với "${keyword.trim()}".`
                  : 'Không có người dùng nào.'}
              </div>
            ) : (
              availableUsers.map((user) => {
                const checked = isSelected(user.id)
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
                      'flex items-center justify-between p-2.5 rounded-lg cursor-pointer transition-colors text-left select-none',
                      checked ? 'bg-primary/5 hover:bg-primary/10' : 'hover:bg-muted/60'
                    )}
                  >
                    <div className='flex items-center gap-3 min-w-0'>
                      <Avatar className='h-9 w-9 shrink-0'>
                        {user.avatarFileId && (
                          <AvatarImage src={`/api/files/${user.avatarFileId}`} alt={user.fullName} />
                        )}
                        <AvatarFallback className='text-xs'>{user.fullName.slice(0, 1).toUpperCase()}</AvatarFallback>
                      </Avatar>
                      <div className='min-w-0'>
                        <p className='text-xs font-semibold truncate leading-tight text-foreground'>{user.fullName}</p>
                        <p className='text-[11px] text-muted-foreground truncate leading-tight mt-0.5'>
                          @{user.username}
                        </p>
                      </div>
                    </div>

                    <div
                      className={cn(
                        'flex h-5 w-5 shrink-0 items-center justify-center rounded-md border transition-colors',
                        checked ? 'bg-primary border-primary text-primary-foreground' : 'border-muted-foreground/30'
                      )}
                    >
                      {checked && <Check className='h-3.5 w-3.5 stroke-[3]' />}
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </ScrollArea>

        {/* Phân trang kết quả tìm kiếm nếu có nhiều hơn 1 trang */}
        {totalPages > 1 && (
          <div className='shrink-0 border-t bg-muted/20 px-3 py-1.5 flex items-center justify-between text-xs'>
            <span className='text-[11px] text-muted-foreground'>
              Trang {searchParams.page + 1}/{totalPages}
            </span>
            <div className='flex items-center gap-1'>
              <Button
                type='button'
                variant='ghost'
                size='icon'
                className='h-6 w-6'
                disabled={searchParams.page <= 0}
                onClick={() => setSearchParams((prev) => ({ ...prev, page: prev.page - 1 }))}
                title='Trang trước'
              >
                <ChevronLeft className='h-3.5 w-3.5' />
              </Button>
              <Button
                type='button'
                variant='ghost'
                size='icon'
                className='h-6 w-6'
                disabled={searchParams.page >= totalPages - 1}
                onClick={() => setSearchParams((prev) => ({ ...prev, page: prev.page + 1 }))}
                title='Trang sau'
              >
                <ChevronRight className='h-3.5 w-3.5' />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <DialogFooter className='shrink-0 border-t p-4 flex flex-col sm:flex-row items-center justify-between gap-2 bg-muted/10'>
        {createGroupMutation.error ? (
          <div className='flex items-center gap-1.5 text-xs text-destructive max-w-xs truncate'>
            <AlertCircle className='h-3.5 w-3.5 shrink-0' />
            <span>{getApiErrorMessage(createGroupMutation.error)}</span>
          </div>
        ) : (
          <div className='text-xs text-muted-foreground'>
            {selectedUsers.length === 0 ? 'Chưa chọn thành viên' : `${selectedUsers.length} thành viên được chọn`}
          </div>
        )}

        <div className='flex items-center gap-2 w-full sm:w-auto justify-end'>
          <Button
            type='button'
            variant='outline'
            size='sm'
            onClick={() => onOpenChange(false)}
            disabled={createGroupMutation.isPending}
          >
            Hủy
          </Button>
          <Button
            type='button'
            size='sm'
            onClick={handleCreate}
            disabled={!trimmedName || selectedUsers.length === 0 || createGroupMutation.isPending}
            className='gap-1.5'
          >
            {createGroupMutation.isPending && <LoaderCircle className='h-3.5 w-3.5 animate-spin' />}
            Tạo nhóm {selectedUsers.length > 0 && `(${selectedUsers.length})`}
          </Button>
        </div>
      </DialogFooter>
    </DialogContent>
  )
}
