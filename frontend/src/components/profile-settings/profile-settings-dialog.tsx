import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { AlertCircle, Camera, LoaderCircle, Trash2, UserRound } from 'lucide-react'
import fileApi from '~/apis/file.api'
import { Avatar, AvatarFallback, AvatarImage } from '~/components/ui/avatar'
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
import { useUpdateProfileMutation } from '~/hooks/use-auth'
import { getApiErrorMessage } from '~/lib/api-error'
import type { User } from '~/types/user.type'
import { canChangeProfileDialogOpenState } from './profile-settings.utils'

const MAX_AVATAR_SIZE = 20 * 1024 * 1024

interface ProfileSettingsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  user: User
}

export const ProfileSettingsDialog = ({ open, onOpenChange, user }: ProfileSettingsDialogProps) => {
  const [isUploading, setIsUploading] = useState(false)

  const handleOpenChange = (nextOpen: boolean) => {
    if (canChangeProfileDialogOpenState(nextOpen, isUploading)) onOpenChange(nextOpen)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      {open && (
        <ProfileSettingsDialogContent
          key={user.id}
          onOpenChange={handleOpenChange}
          onUploadingChange={setIsUploading}
          user={user}
        />
      )}
    </Dialog>
  )
}

interface ProfileSettingsDialogContentProps {
  onOpenChange: (open: boolean) => void
  onUploadingChange: (isUploading: boolean) => void
  user: User
}

const ProfileSettingsDialogContent = ({ onOpenChange, onUploadingChange, user }: ProfileSettingsDialogContentProps) => {
  const updateProfileMutation = useUpdateProfileMutation()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const localPreviewUrlRef = useRef<string | null>(null)
  const [fullName, setFullName] = useState(user.fullName)
  const [avatarFileId, setAvatarFileId] = useState<string | null>(user.avatarFileId)
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState<string | null>(
    user.avatarFileId ? `/api/files/${user.avatarFileId}` : null
  )
  const [isUploading, setIsUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)

  useEffect(
    () => () => {
      if (localPreviewUrlRef.current) URL.revokeObjectURL(localPreviewUrlRef.current)
    },
    []
  )

  const trimmedFullName = fullName.trim()
  const fullNameError = !trimmedFullName ? 'Họ và tên không được để trống.' : undefined
  const hasChanges = trimmedFullName !== user.fullName || avatarFileId !== user.avatarFileId

  const clearLocalPreview = () => {
    if (!localPreviewUrlRef.current) return
    URL.revokeObjectURL(localPreviewUrlRef.current)
    localPreviewUrlRef.current = null
  }

  const handleSelectAvatar = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    if (!file.type.startsWith('image/')) {
      setUploadError('Vui lòng chọn tệp hình ảnh.')
      event.target.value = ''
      return
    }
    if (file.size === 0) {
      setUploadError('Không thể tải lên ảnh rỗng.')
      event.target.value = ''
      return
    }
    if (file.size > MAX_AVATAR_SIZE) {
      setUploadError('Dung lượng ảnh không được vượt quá 20 MiB.')
      event.target.value = ''
      return
    }

    clearLocalPreview()
    const localPreviewUrl = URL.createObjectURL(file)
    localPreviewUrlRef.current = localPreviewUrl
    setAvatarPreviewUrl(localPreviewUrl)
    setUploadError(null)
    setIsUploading(true)
    onUploadingChange(true)

    try {
      const response = await fileApi.upload(file)
      const uploadedFile = response.data.content
      clearLocalPreview()
      setAvatarFileId(uploadedFile.id)
      setAvatarPreviewUrl(uploadedFile.downloadUrl)
    } catch (error) {
      clearLocalPreview()
      setAvatarPreviewUrl(avatarFileId ? `/api/files/${avatarFileId}` : null)
      setUploadError(getApiErrorMessage(error) || 'Không thể tải ảnh lên. Vui lòng thử lại.')
    } finally {
      setIsUploading(false)
      onUploadingChange(false)
      event.target.value = ''
    }
  }

  const handleRemoveAvatar = () => {
    clearLocalPreview()
    setAvatarPreviewUrl(null)
    setAvatarFileId(null)
    setUploadError(null)
  }

  const handleSave = async () => {
    if (!trimmedFullName || isUploading || !hasChanges) return

    try {
      await updateProfileMutation.mutateAsync({ fullName: trimmedFullName, avatarFileId })
      onOpenChange(false)
    } catch {
      // Mutation error is rendered below the form.
    }
  }

  const initials = (trimmedFullName || user.username || 'U').slice(0, 1).toUpperCase()
  const isPending = isUploading || updateProfileMutation.isPending

  return (
    <DialogContent className='w-[calc(100%_-_2rem)] max-w-md gap-0 overflow-hidden p-0 sm:rounded-xl'>
      <DialogHeader className='border-b p-5 pr-12'>
        <div className='flex items-center gap-2'>
          <div className='flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary'>
            <UserRound className='h-4 w-4' aria-hidden='true' />
          </div>
          <div>
            <DialogTitle>Chỉnh sửa hồ sơ</DialogTitle>
            <DialogDescription className='mt-0.5 text-xs'>
              Cập nhật tên hiển thị và ảnh đại diện của bạn.
            </DialogDescription>
          </div>
        </div>
      </DialogHeader>

      <div className='space-y-5 p-5'>
        <div className='flex flex-col items-center gap-3'>
          <div className='relative'>
            <Avatar className='h-20 w-20 border-2 border-border shadow-sm'>
              {avatarPreviewUrl && <AvatarImage src={avatarPreviewUrl} alt={trimmedFullName || user.username} />}
              <AvatarFallback className='bg-primary/10 text-xl font-bold text-primary'>{initials}</AvatarFallback>
            </Avatar>
            <input
              ref={fileInputRef}
              type='file'
              accept='image/*'
              className='hidden'
              onChange={handleSelectAvatar}
              disabled={isPending}
              aria-label='Chọn ảnh đại diện'
            />
            <button
              type='button'
              onClick={() => fileInputRef.current?.click()}
              disabled={isPending}
              className='absolute bottom-0 right-0 flex h-7 w-7 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-md transition-transform hover:bg-primary/90 active:scale-95 disabled:opacity-50'
              title='Đổi ảnh đại diện'
              aria-label='Đổi ảnh đại diện'
            >
              {isUploading ? (
                <LoaderCircle className='h-3.5 w-3.5 animate-spin' aria-hidden='true' />
              ) : (
                <Camera className='h-3.5 w-3.5' aria-hidden='true' />
              )}
            </button>
          </div>

          {avatarPreviewUrl && (
            <Button
              type='button'
              variant='ghost'
              size='sm'
              onClick={handleRemoveAvatar}
              disabled={isPending}
              className='h-7 gap-1 text-xs text-destructive hover:bg-destructive/10 hover:text-destructive'
            >
              <Trash2 className='h-3 w-3' aria-hidden='true' />
              Xóa ảnh đại diện
            </Button>
          )}

          {isUploading && (
            <p className='sr-only' role='status'>
              Đang tải ảnh đại diện...
            </p>
          )}
          {uploadError && (
            <p className='text-center text-xs text-destructive' role='alert'>
              {uploadError}
            </p>
          )}
        </div>

        <div className='space-y-1.5'>
          <label htmlFor='profile-full-name' className='text-xs font-semibold text-foreground'>
            Họ và tên <span className='text-destructive'>*</span>
          </label>
          <Input
            id='profile-full-name'
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            disabled={isPending}
            autoComplete='name'
            aria-invalid={Boolean(fullNameError)}
            aria-describedby={fullNameError ? 'profile-full-name-error' : undefined}
          />
          {fullNameError && (
            <p id='profile-full-name-error' className='text-xs text-destructive' role='alert'>
              {fullNameError}
            </p>
          )}
        </div>

        <div className='rounded-md border bg-muted/30 px-3 py-2 text-xs text-muted-foreground'>
          Tài khoản: <span className='font-medium text-foreground'>@{user.username}</span>
        </div>

        {updateProfileMutation.error && (
          <div
            className='flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-xs text-destructive'
            role='alert'
          >
            <AlertCircle className='mt-0.5 h-3.5 w-3.5 shrink-0' aria-hidden='true' />
            <span>{getApiErrorMessage(updateProfileMutation.error)}</span>
          </div>
        )}
      </div>

      <DialogFooter className='border-t bg-muted/10 p-4'>
        <Button type='button' variant='outline' size='sm' onClick={() => onOpenChange(false)} disabled={isPending}>
          Hủy
        </Button>
        <Button
          type='button'
          size='sm'
          onClick={handleSave}
          disabled={Boolean(fullNameError) || !hasChanges || isPending}
        >
          {updateProfileMutation.isPending && <LoaderCircle className='h-3.5 w-3.5 animate-spin' aria-hidden='true' />}
          Lưu thay đổi
        </Button>
      </DialogFooter>
    </DialogContent>
  )
}
