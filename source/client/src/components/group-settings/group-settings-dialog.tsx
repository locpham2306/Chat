import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  AlertCircle,
  Calendar,
  Camera,
  Check,
  ChevronLeft,
  ChevronRight,
  Crown,
  LoaderCircle,
  LogOut,
  Search,
  Settings,
  ShieldAlert,
  Trash2,
  UserMinus,
  UserPlus,
  Users
} from 'lucide-react'
import fileApi from '~/apis/file.api'
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
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '~/components/ui/tooltip'
import { useConversationDetailQuery, useUpdateConversationMutation } from '~/hooks/use-conversation'
import { useGroupMembersQuery, useRemoveGroupMemberMutation } from '~/hooks/use-group-members'
import { getApiErrorMessage } from '~/lib/api-error'
import { cn } from '~/lib/utils'
import { useAuthStore } from '~/stores/use-auth-store'
import type { ConversationDetail, ConversationMember } from '~/types/conversation.type'
import { AddMembersDialog } from './add-members-dialog'

interface GroupSettingsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  conversation: ConversationDetail
}

export const GroupSettingsDialog = ({ open, onOpenChange, conversation }: GroupSettingsDialogProps) => {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {open && <GroupSettingsDialogContent onOpenChange={onOpenChange} conversation={conversation} />}
    </Dialog>
  )
}

interface GroupSettingsDialogContentProps {
  onOpenChange: (open: boolean) => void
  conversation: ConversationDetail
}

const formatGroupDate = (dateString?: string) => {
  if (!dateString) return ''
  try {
    return new Date(dateString).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    })
  } catch {
    return dateString
  }
}

const MEMBERS_PAGE_SIZE = 20

const GroupSettingsDialogContent = ({ onOpenChange, conversation }: GroupSettingsDialogContentProps) => {
  const navigate = useNavigate()
  const currentUser = useAuthStore((state) => state.user)

  // Lấy dữ liệu hội thoại mới nhất từ cache
  const { data: latestConversation } = useConversationDetailQuery(conversation.id)
  const currentConv = latestConversation || conversation

  const isOwner = Boolean(currentUser?.id && currentConv.ownerId && currentUser.id === currentConv.ownerId)

  // Tabs
  const [activeTab, setActiveTab] = useState<'info' | 'members'>('info')

  // Tab Info States
  const updateMutation = useUpdateConversationMutation()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [name, setName] = useState(currentConv.name || '')
  const [nameTouched, setNameTouched] = useState(false)
  const [avatarFileId, setAvatarFileId] = useState<string | null>(currentConv.avatarFileId)
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState<string | null>(
    currentConv.avatarFileId ? `/api/files/${currentConv.avatarFileId}` : null
  )
  const [isUploading, setIsUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [isSavedSuccessfully, setIsSavedSuccessfully] = useState(false)

  // Tab Members States
  const [membersPage, setMembersPage] = useState(0)
  const [memberKeyword, setMemberKeyword] = useState('')
  const [isAddMembersOpen, setIsAddMembersOpen] = useState(false)
  const [memberToRemove, setMemberToRemove] = useState<ConversationMember | null>(null)
  const [isConfirmLeaveOpen, setIsConfirmLeaveOpen] = useState(false)

  const membersQuery = useGroupMembersQuery(
    currentConv.id,
    { page: membersPage, size: MEMBERS_PAGE_SIZE },
    { enabled: activeTab === 'members' }
  )
  const removeMemberMutation = useRemoveGroupMemberMutation(currentConv.id)

  const trimmedName = name.trim()
  const nameError = nameTouched && !trimmedName ? 'Tên nhóm không được để trống' : ''
  const isNameChanged = trimmedName !== (currentConv.name || '').trim()
  const isAvatarChanged = avatarFileId !== currentConv.avatarFileId
  const hasChanges = isNameChanged || isAvatarChanged

  // Xử lý upload avatar nhóm
  const handleSelectFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.type.startsWith('image/')) {
      setUploadError('Vui lòng chọn tệp định dạng hình ảnh (PNG, JPG, WEBP...)')
      return
    }

    if (file.size > 20 * 1024 * 1024) {
      setUploadError('Dung lượng ảnh không được vượt quá 20MB')
      return
    }

    setUploadError(null)
    setIsUploading(true)

    const localUrl = URL.createObjectURL(file)
    setAvatarPreviewUrl(localUrl)

    try {
      const response = await fileApi.upload(file)
      const uploadedFile = response.data.content
      setAvatarFileId(uploadedFile.id)
    } catch (err) {
      setUploadError(getApiErrorMessage(err) || 'Không thể tải ảnh lên. Vui lòng thử lại.')
      setAvatarPreviewUrl(currentConv.avatarFileId ? `/api/files/${currentConv.avatarFileId}` : null)
      setAvatarFileId(currentConv.avatarFileId)
    } finally {
      setIsUploading(false)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  const handleRemoveAvatar = () => {
    setUploadError(null)
    setAvatarFileId(null)
    setAvatarPreviewUrl(null)
  }

  const handleSaveInfo = async () => {
    if (!isOwner) return
    setNameTouched(true)
    if (!trimmedName || !hasChanges) return

    try {
      await updateMutation.mutateAsync({
        id: currentConv.id,
        body: {
          name: trimmedName,
          avatarFileId
        }
      })
      setIsSavedSuccessfully(true)
      setTimeout(() => {
        setIsSavedSuccessfully(false)
      }, 2000)
    } catch {
      // Error handled by mutation
    }
  }

  // Xử lý xóa thành viên
  const handleConfirmRemoveMember = async () => {
    if (!memberToRemove) return
    try {
      await removeMemberMutation.mutateAsync(memberToRemove.user.id)
      setMemberToRemove(null)
    } catch {
      // Error handled by mutation
    }
  }

  // Xử lý rời nhóm
  const handleConfirmLeaveGroup = async () => {
    if (!currentUser || isOwner) return
    try {
      await removeMemberMutation.mutateAsync(currentUser.id)
      setIsConfirmLeaveOpen(false)
      onOpenChange(false)
      navigate('/chat', { replace: true })
    } catch {
      // Error handled by mutation
    }
  }

  // Filter thành viên phía client nếu có từ khóa
  const membersList = membersQuery.data?.data ?? []
  const filteredMembers = memberKeyword.trim()
    ? membersList.filter((m) => {
        const query = memberKeyword.trim().toLowerCase()
        return m.user.fullName.toLowerCase().includes(query) || m.user.username.toLowerCase().includes(query)
      })
    : membersList

  const totalMemberPages = membersQuery.data?.totalPages ?? 0

  return (
    <DialogContent className='flex max-h-[90vh] w-[calc(100%_-_2rem)] max-w-lg flex-col gap-0 overflow-hidden p-0 sm:rounded-xl'>
      <DialogHeader className='shrink-0 border-b p-5 pr-12'>
        <div className='flex items-center gap-2'>
          <div className='flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary'>
            <Users className='h-4 w-4' />
          </div>
          <div>
            <DialogTitle>{currentConv.name || 'Cài đặt nhóm'}</DialogTitle>
            <DialogDescription className='text-xs mt-0.5'>
              Quản lý thông tin và thành viên của nhóm trò chuyện.
            </DialogDescription>
          </div>
        </div>
      </DialogHeader>

      {/* Tabs navigation */}
      <div className='flex border-b px-5 bg-muted/20 shrink-0' role='tablist' aria-label='Cài đặt nhóm'>
        <button
          type='button'
          role='tab'
          id='tab-group-info'
          aria-selected={activeTab === 'info'}
          aria-controls='tabpanel-group-info'
          tabIndex={activeTab === 'info' ? 0 : -1}
          onClick={() => setActiveTab('info')}
          onKeyDown={(e) => {
            if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
              e.preventDefault()
              setActiveTab('members')
            }
          }}
          className={cn(
            'flex items-center gap-2 py-3 px-3 text-xs font-semibold border-b-2 transition-colors -mb-px focus:outline-hidden focus:ring-1 focus:ring-ring',
            activeTab === 'info'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted-foreground hover:text-foreground'
          )}
        >
          <Settings className='h-3.5 w-3.5' aria-hidden='true' />
          Cài đặt chung
        </button>
        <button
          type='button'
          role='tab'
          id='tab-group-members'
          aria-selected={activeTab === 'members'}
          aria-controls='tabpanel-group-members'
          tabIndex={activeTab === 'members' ? 0 : -1}
          onClick={() => setActiveTab('members')}
          onKeyDown={(e) => {
            if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
              e.preventDefault()
              setActiveTab('info')
            }
          }}
          className={cn(
            'flex items-center gap-2 py-3 px-3 text-xs font-semibold border-b-2 transition-colors -mb-px focus:outline-hidden focus:ring-1 focus:ring-ring',
            activeTab === 'members'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted-foreground hover:text-foreground'
          )}
        >
          <Users className='h-3.5 w-3.5' aria-hidden='true' />
          Thành viên ({currentConv.memberCount})
        </button>
      </div>

      {/* Tab 1: Cài đặt chung */}
      {activeTab === 'info' && (
        <div
          role='tabpanel'
          id='tabpanel-group-info'
          aria-labelledby='tab-group-info'
          tabIndex={0}
          className='flex-1 overflow-y-auto p-5 space-y-5 focus:outline-hidden'
        >
          {/* Vai trò của người dùng */}
          <div className='flex items-center justify-between p-3 rounded-lg bg-muted/40 border text-xs'>
            <span className='text-muted-foreground'>Vai trò của bạn:</span>
            {isOwner ? (
              <Badge variant='default' className='gap-1 bg-amber-500 hover:bg-amber-600 text-white font-medium'>
                <Crown className='h-3 w-3' />
                Chủ nhóm (Owner)
              </Badge>
            ) : (
              <Badge variant='secondary' className='gap-1 font-medium'>
                Thành viên
              </Badge>
            )}
          </div>

          {!isOwner && (
            <div className='flex items-start gap-2.5 p-3 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-700 dark:text-amber-400 text-xs'>
              <ShieldAlert className='h-4 w-4 shrink-0 mt-0.5' />
              <p className='leading-relaxed'>
                Chỉ <strong>Chủ nhóm (Owner)</strong> mới có quyền chỉnh sửa tên và thay đổi ảnh đại diện của nhóm.
              </p>
            </div>
          )}

          {/* Avatar nhóm */}
          <div className='flex flex-col items-center justify-center gap-3 py-2'>
            <div className='relative group'>
              <Avatar className='h-20 w-20 border-2 border-border shadow-sm'>
                {avatarPreviewUrl && <AvatarImage src={avatarPreviewUrl} alt={name || 'Nhóm'} />}
                <AvatarFallback className='bg-primary/10 text-primary text-xl font-bold'>
                  {(trimmedName || 'N').slice(0, 1).toUpperCase()}
                </AvatarFallback>
              </Avatar>

              {isOwner && (
                <>
                  <input
                    ref={fileInputRef}
                    type='file'
                    accept='image/*'
                    className='hidden'
                    onChange={handleSelectFile}
                    disabled={isUploading || updateMutation.isPending}
                  />
                  <button
                    type='button'
                    onClick={() => fileInputRef.current?.click()}
                    disabled={isUploading || updateMutation.isPending}
                    className='absolute bottom-0 right-0 flex h-7 w-7 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-md hover:bg-primary/90 transition-transform active:scale-95 disabled:opacity-50'
                    title='Đổi ảnh đại diện'
                    aria-label='Đổi ảnh đại diện nhóm'
                  >
                    {isUploading ? (
                      <LoaderCircle className='h-3.5 w-3.5 animate-spin' />
                    ) : (
                      <Camera className='h-3.5 w-3.5' />
                    )}
                  </button>
                </>
              )}
            </div>

            {isOwner && avatarPreviewUrl && (
              <Button
                type='button'
                variant='ghost'
                size='sm'
                onClick={handleRemoveAvatar}
                disabled={isUploading || updateMutation.isPending}
                className='h-7 text-xs text-destructive hover:text-destructive hover:bg-destructive/10 gap-1'
              >
                <Trash2 className='h-3 w-3' />
                Xóa ảnh đại diện
              </Button>
            )}

            {uploadError && <p className='text-xs text-destructive text-center'>{uploadError}</p>}
          </div>

          {/* Tên nhóm */}
          <div className='space-y-1.5'>
            <label
              htmlFor='settings-group-name'
              className='text-xs font-semibold text-foreground flex items-center justify-between'
            >
              <span>Tên nhóm {isOwner && <span className='text-destructive'>*</span>}</span>
              {isOwner && <span className='text-[10px] text-muted-foreground font-normal'>{name.length}/100</span>}
            </label>
            <Input
              id='settings-group-name'
              value={name}
              maxLength={100}
              aria-invalid={Boolean(nameError)}
              aria-describedby={nameError ? 'settings-group-name-error' : undefined}
              disabled={!isOwner || updateMutation.isPending}
              onChange={(e) => {
                setName(e.target.value)
                if (!nameTouched) setNameTouched(true)
              }}
              placeholder='Nhập tên nhóm...'
              className={!isOwner ? 'bg-muted cursor-not-allowed opacity-80' : ''}
            />
            {nameError && (
              <p id='settings-group-name-error' className='text-xs text-destructive' role='alert'>
                {nameError}
              </p>
            )}
          </div>

          {/* Thống kê nhóm */}
          <div className='space-y-2 pt-2 border-t text-xs text-muted-foreground'>
            <div className='flex items-center justify-between'>
              <span className='flex items-center gap-1.5'>
                <Users className='h-3.5 w-3.5' />
                Số lượng thành viên:
              </span>
              <span className='font-semibold text-foreground'>{currentConv.memberCount} thành viên</span>
            </div>
            <div className='flex items-center justify-between'>
              <span className='flex items-center gap-1.5'>
                <Calendar className='h-3.5 w-3.5' />
                Ngày tạo nhóm:
              </span>
              <span className='text-foreground'>{formatGroupDate(currentConv.createdAt)}</span>
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Quản lý thành viên */}
      {activeTab === 'members' && (
        <div
          role='tabpanel'
          id='tabpanel-group-members'
          aria-labelledby='tab-group-members'
          tabIndex={0}
          className='flex flex-1 flex-col overflow-hidden focus:outline-hidden'
        >
          {/* Header hành động của Tab Members */}
          <div className='p-3 border-b flex items-center justify-between gap-2 shrink-0 bg-muted/10'>
            <div className='relative flex-1'>
              <Search className='absolute top-1/2 left-3 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground' />
              <Input
                value={memberKeyword}
                onChange={(e) => setMemberKeyword(e.target.value)}
                placeholder='Lọc thành viên...'
                aria-label='Lọc thành viên trong nhóm'
                className='pl-8 h-8 text-xs'
              />
            </div>

            {isOwner && (
              <Button
                type='button'
                size='sm'
                onClick={() => setIsAddMembersOpen(true)}
                className='h-8 text-xs gap-1.5 shrink-0'
              >
                <UserPlus className='h-3.5 w-3.5' />
                Thêm thành viên
              </Button>
            )}
          </div>

          {/* Danh sách thành viên */}
          <ScrollArea className='flex-1'>
            <div className='p-3'>
              {membersQuery.isPending ? (
                <div className='flex items-center justify-center py-12 text-xs text-muted-foreground'>
                  <LoaderCircle className='mr-2 h-4 w-4 animate-spin' />
                  <span>Đang tải danh sách thành viên...</span>
                </div>
              ) : membersQuery.isError ? (
                <div className='flex flex-col items-center justify-center py-10 text-center'>
                  <p className='text-xs text-destructive'>{getApiErrorMessage(membersQuery.error)}</p>
                  <Button variant='ghost' size='sm' onClick={() => membersQuery.refetch()} className='mt-2 text-xs'>
                    Thử lại
                  </Button>
                </div>
              ) : filteredMembers.length === 0 ? (
                <div className='flex flex-col items-center justify-center py-12 text-center text-muted-foreground'>
                  <p className='text-xs'>Không tìm thấy thành viên phù hợp</p>
                </div>
              ) : (
                <div className='space-y-1.5'>
                  {filteredMembers.map((member) => {
                    const isMemberOwner = member.role === 'OWNER'
                    const isSelf = member.user.id === currentUser?.id
                    const initials = (member.user.fullName.trim().slice(0, 1) || 'U').toUpperCase()
                    const avatarUrl = member.user.avatarFileId ? `/api/files/${member.user.avatarFileId}` : null

                    return (
                      <div
                        key={member.user.id}
                        className='flex items-center justify-between p-2.5 rounded-lg border border-border/50 bg-card hover:bg-muted/40 transition-colors'
                      >
                        <div className='flex items-center gap-3 min-w-0'>
                          <div className='relative shrink-0'>
                            <Avatar className='h-9 w-9'>
                              {avatarUrl && <AvatarImage src={avatarUrl} alt={member.user.fullName} />}
                              <AvatarFallback className='bg-primary/10 text-primary font-semibold text-xs'>
                                {initials}
                              </AvatarFallback>
                            </Avatar>
                            {member.user.status === 'ONLINE' && (
                              <span
                                className='absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full bg-emerald-500 ring-2 ring-background'
                                title='Đang trực tuyến'
                              />
                            )}
                          </div>

                          <div className='min-w-0'>
                            <div className='flex items-center gap-1.5'>
                              <p className='text-xs font-semibold truncate leading-tight'>{member.user.fullName}</p>
                              {isSelf && <span className='text-[10px] text-muted-foreground font-normal'>(Bạn)</span>}
                            </div>
                            <p className='text-[11px] text-muted-foreground truncate leading-tight mt-0.5'>
                              @{member.user.username}
                            </p>
                          </div>
                        </div>

                        <div className='flex items-center gap-2 shrink-0'>
                          {isMemberOwner ? (
                            <Badge
                              variant='default'
                              className='gap-1 bg-amber-500 hover:bg-amber-600 text-white text-[10px] font-medium h-5 px-1.5'
                            >
                              <Crown className='h-3 w-3' />
                              Chủ nhóm
                            </Badge>
                          ) : (
                            <Badge variant='secondary' className='text-[10px] font-medium h-5 px-1.5'>
                              Thành viên
                            </Badge>
                          )}

                          {/* Nút xóa thành viên (chỉ Owner có quyền xóa Member khác) */}
                          {isOwner && !isMemberOwner && !isSelf && (
                            <Button
                              type='button'
                              variant='ghost'
                              size='icon'
                              className='h-7 w-7 text-muted-foreground hover:text-destructive hover:bg-destructive/10'
                              onClick={() => setMemberToRemove(member)}
                              title={`Xóa ${member.user.fullName} khỏi nhóm`}
                              aria-label={`Xóa ${member.user.fullName} khỏi nhóm`}
                            >
                              <UserMinus className='h-3.5 w-3.5' />
                            </Button>
                          )}
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </ScrollArea>

          {/* Phân trang danh sách thành viên */}
          {totalMemberPages > 1 && (
            <div className='flex items-center justify-between border-t p-2 px-4 text-xs text-muted-foreground shrink-0 bg-muted/10'>
              <span>
                Trang {membersPage + 1} / {totalMemberPages}
              </span>
              <div className='flex items-center gap-1'>
                <Button
                  variant='ghost'
                  size='icon'
                  className='h-7 w-7'
                  disabled={membersPage === 0 || membersQuery.isPending}
                  onClick={() => setMembersPage((p) => Math.max(0, p - 1))}
                  aria-label='Trang trước'
                >
                  <ChevronLeft className='h-3.5 w-3.5' />
                </Button>
                <Button
                  variant='ghost'
                  size='icon'
                  className='h-7 w-7'
                  disabled={membersPage + 1 >= totalMemberPages || membersQuery.isPending}
                  onClick={() => setMembersPage((p) => p + 1)}
                  aria-label='Trang tiếp'
                >
                  <ChevronRight className='h-3.5 w-3.5' />
                </Button>
              </div>
            </div>
          )}

          {/* Khu vực nút Rời nhóm ở cuối */}
          <div className='p-3 border-t bg-muted/10 shrink-0'>
            {isOwner ? (
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <div className='w-full'>
                      <Button
                        type='button'
                        variant='outline'
                        disabled
                        className='w-full text-xs opacity-60 cursor-not-allowed gap-2'
                      >
                        <LogOut className='h-3.5 w-3.5' />
                        Rời nhóm chat (Chủ nhóm không thể rời)
                      </Button>
                    </div>
                  </TooltipTrigger>
                  <TooltipContent>
                    <p className='text-xs'>Chủ nhóm không được rời nhóm trong phạm vi hiện tại.</p>
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            ) : (
              <Button
                type='button'
                variant='outline'
                onClick={() => setIsConfirmLeaveOpen(true)}
                className='w-full text-xs text-destructive border-destructive/30 hover:bg-destructive/10 hover:text-destructive gap-2'
              >
                <LogOut className='h-3.5 w-3.5' />
                Rời nhóm chat
              </Button>
            )}
          </div>
        </div>
      )}

      {/* Footer chung */}
      {activeTab === 'info' && (
        <DialogFooter className='shrink-0 border-t p-4 flex flex-col sm:flex-row items-center justify-between gap-2 bg-muted/10'>
          {updateMutation.error ? (
            <div className='flex items-center gap-1.5 text-xs text-destructive max-w-xs truncate'>
              <AlertCircle className='h-3.5 w-3.5 shrink-0' />
              <span>{getApiErrorMessage(updateMutation.error)}</span>
            </div>
          ) : isSavedSuccessfully ? (
            <div className='flex items-center gap-1.5 text-xs text-emerald-600 font-medium'>
              <Check className='h-3.5 w-3.5' />
              <span>Đã cập nhật thành công!</span>
            </div>
          ) : (
            <div />
          )}

          <div className='flex items-center gap-2 w-full sm:w-auto justify-end'>
            <Button
              type='button'
              variant='outline'
              size='sm'
              onClick={() => onOpenChange(false)}
              disabled={updateMutation.isPending}
            >
              {isOwner ? 'Hủy' : 'Đóng'}
            </Button>

            {isOwner && (
              <Button
                type='button'
                size='sm'
                onClick={handleSaveInfo}
                disabled={!trimmedName || !hasChanges || isUploading || updateMutation.isPending}
                className='gap-1.5'
              >
                {updateMutation.isPending && <LoaderCircle className='h-3.5 w-3.5 animate-spin' />}
                Lưu thay đổi
              </Button>
            )}
          </div>
        </DialogFooter>
      )}

      {/* Dialog xác nhận xóa thành viên khỏi nhóm */}
      <Dialog open={Boolean(memberToRemove)} onOpenChange={(open) => !open && setMemberToRemove(null)}>
        <DialogContent
          className='max-w-sm'
          role='alertdialog'
          aria-labelledby='remove-member-dialog-title'
          aria-describedby='remove-member-dialog-desc'
        >
          <DialogHeader>
            <DialogTitle id='remove-member-dialog-title' className='text-sm'>
              Xóa thành viên khỏi nhóm?
            </DialogTitle>
            <DialogDescription id='remove-member-dialog-desc' className='text-xs leading-relaxed'>
              Bạn có chắc chắn muốn xóa <strong>{memberToRemove?.user.fullName}</strong> khỏi nhóm trò chuyện này không?
            </DialogDescription>
          </DialogHeader>

          {removeMemberMutation.error && (
            <p className='text-xs text-destructive'>{getApiErrorMessage(removeMemberMutation.error)}</p>
          )}

          <DialogFooter className='gap-2 sm:gap-0 mt-2'>
            <Button
              variant='outline'
              size='sm'
              onClick={() => setMemberToRemove(null)}
              disabled={removeMemberMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              variant='destructive'
              size='sm'
              onClick={handleConfirmRemoveMember}
              disabled={removeMemberMutation.isPending}
              className='gap-1.5'
            >
              {removeMemberMutation.isPending && <LoaderCircle className='h-3.5 w-3.5 animate-spin' />}
              Xác nhận xóa
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Dialog xác nhận rời nhóm */}
      <Dialog open={isConfirmLeaveOpen} onOpenChange={setIsConfirmLeaveOpen}>
        <DialogContent
          className='max-w-sm'
          role='alertdialog'
          aria-labelledby='leave-group-dialog-title'
          aria-describedby='leave-group-dialog-desc'
        >
          <DialogHeader>
            <DialogTitle id='leave-group-dialog-title' className='text-sm'>
              Rời khỏi nhóm trò chuyện?
            </DialogTitle>
            <DialogDescription id='leave-group-dialog-desc' className='text-xs leading-relaxed'>
              Bạn sẽ không còn nhận được tin nhắn từ nhóm <strong>{currentConv.name}</strong> nữa.
            </DialogDescription>
          </DialogHeader>

          {removeMemberMutation.error && (
            <p className='text-xs text-destructive'>{getApiErrorMessage(removeMemberMutation.error)}</p>
          )}

          <DialogFooter className='gap-2 sm:gap-0 mt-2'>
            <Button
              variant='outline'
              size='sm'
              onClick={() => setIsConfirmLeaveOpen(false)}
              disabled={removeMemberMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              variant='destructive'
              size='sm'
              onClick={handleConfirmLeaveGroup}
              disabled={removeMemberMutation.isPending}
              className='gap-1.5'
            >
              {removeMemberMutation.isPending && <LoaderCircle className='h-3.5 w-3.5 animate-spin' />}
              Rời nhóm
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Modal Thêm thành viên */}
      <AddMembersDialog open={isAddMembersOpen} onOpenChange={setIsAddMembersOpen} conversationId={currentConv.id} />
    </DialogContent>
  )
}
