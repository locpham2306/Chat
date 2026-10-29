import { useRef, useState } from 'react'
import { AlertCircle, Camera, Calendar, Check, Crown, LoaderCircle, ShieldAlert, Trash2, Users } from 'lucide-react'
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
import { useUpdateConversationMutation } from '~/hooks/use-conversation'
import { getApiErrorMessage } from '~/lib/api-error'
import { useAuthStore } from '~/stores/use-auth-store'
import type { ConversationDetail } from '~/types/conversation.type'

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

const GroupSettingsDialogContent = ({ onOpenChange, conversation }: GroupSettingsDialogContentProps) => {
  const currentUser = useAuthStore((state) => state.user)
  const isOwner = Boolean(currentUser?.id && conversation.ownerId && currentUser.id === conversation.ownerId)

  const updateMutation = useUpdateConversationMutation()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [name, setName] = useState(conversation.name || '')
  const [nameTouched, setNameTouched] = useState(false)
  const [avatarFileId, setAvatarFileId] = useState<string | null>(conversation.avatarFileId)
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState<string | null>(
    conversation.avatarFileId ? `/api/files/${conversation.avatarFileId}` : null
  )
  const [isUploading, setIsUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [isSavedSuccessfully, setIsSavedSuccessfully] = useState(false)

  const trimmedName = name.trim()
  const nameError = nameTouched && !trimmedName ? 'Tên nhóm không được để trống' : ''

  const isNameChanged = trimmedName !== (conversation.name || '').trim()
  const isAvatarChanged = avatarFileId !== conversation.avatarFileId
  const hasChanges = isNameChanged || isAvatarChanged

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

    // Tạo preview ngay lập tức
    const localUrl = URL.createObjectURL(file)
    setAvatarPreviewUrl(localUrl)

    try {
      const response = await fileApi.upload(file)
      const uploadedFile = response.data.content
      setAvatarFileId(uploadedFile.id)
    } catch (err) {
      setUploadError(getApiErrorMessage(err) || 'Không thể tải ảnh lên. Vui lòng thử lại.')
      // Rollback preview
      setAvatarPreviewUrl(conversation.avatarFileId ? `/api/files/${conversation.avatarFileId}` : null)
      setAvatarFileId(conversation.avatarFileId)
    } finally {
      setIsUploading(false)
      // Reset input file để có thể chọn lại cùng 1 file nếu muốn
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

  const handleSave = async () => {
    if (!isOwner) return
    setNameTouched(true)
    if (!trimmedName || !hasChanges) return

    try {
      await updateMutation.mutateAsync({
        id: conversation.id,
        body: {
          name: trimmedName,
          avatarFileId
        }
      })
      setIsSavedSuccessfully(true)
      setTimeout(() => {
        onOpenChange(false)
      }, 500)
    } catch {
      // Error handled by mutation
    }
  }

  return (
    <DialogContent className='flex max-h-[90vh] w-[calc(100%_-_2rem)] max-w-md flex-col gap-0 overflow-hidden p-0 sm:rounded-xl'>
      <DialogHeader className='shrink-0 border-b p-5 pr-12'>
        <div className='flex items-center gap-2'>
          <div className='flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary'>
            <Users className='h-4 w-4' />
          </div>
          <div>
            <DialogTitle>Cài đặt nhóm chat</DialogTitle>
            <DialogDescription className='text-xs mt-0.5'>
              {isOwner ? 'Quản lý tên và ảnh đại diện của nhóm chat.' : 'Xem thông tin chi tiết nhóm chat.'}
            </DialogDescription>
          </div>
        </div>
      </DialogHeader>

      <div className='flex-1 overflow-y-auto p-5 space-y-5'>
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

        {/* Thông báo nếu không phải OWNER */}
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
            disabled={!isOwner || updateMutation.isPending}
            onChange={(e) => {
              setName(e.target.value)
              if (!nameTouched) setNameTouched(true)
            }}
            placeholder='Nhập tên nhóm...'
            className={!isOwner ? 'bg-muted cursor-not-allowed opacity-80' : ''}
          />
          {nameError && <p className='text-xs text-destructive'>{nameError}</p>}
        </div>

        {/* Thông tin thống kê nhóm */}
        <div className='space-y-2 pt-2 border-t text-xs text-muted-foreground'>
          <div className='flex items-center justify-between'>
            <span className='flex items-center gap-1.5'>
              <Users className='h-3.5 w-3.5' />
              Số lượng thành viên:
            </span>
            <span className='font-semibold text-foreground'>{conversation.memberCount} thành viên</span>
          </div>
          <div className='flex items-center justify-between'>
            <span className='flex items-center gap-1.5'>
              <Calendar className='h-3.5 w-3.5' />
              Ngày tạo nhóm:
            </span>
            <span className='text-foreground'>{formatGroupDate(conversation.createdAt)}</span>
          </div>
        </div>
      </div>

      {/* Footer */}
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
              onClick={handleSave}
              disabled={!trimmedName || !hasChanges || isUploading || updateMutation.isPending}
              className='gap-1.5'
            >
              {updateMutation.isPending && <LoaderCircle className='h-3.5 w-3.5 animate-spin' />}
              Lưu thay đổi
            </Button>
          )}
        </div>
      </DialogFooter>
    </DialogContent>
  )
}
