import { useState, useRef, useEffect, type ChangeEvent, type KeyboardEvent, type FormEvent } from 'react'
import { AlertCircle, FileIcon, LoaderCircle, Maximize2, Paperclip, RotateCcw, SendHorizontal, X } from 'lucide-react'
import { ImageLightbox, type LightboxImage } from '~/components/chat-layout/image-lightbox'
import { Button } from '~/components/ui/button'
import { Textarea } from '~/components/ui/textarea'
import { useFileAttachments } from '~/hooks/use-file-attachments'
import type { MessageSendDraft } from '~/lib/message-send'

interface ChatComposerProps {
  disabled?: boolean
  placeholder?: string
  onSendMessage?: (draft: MessageSendDraft) => boolean | void
  onAttachFile?: () => void
  onTyping?: () => void
  onStopTyping?: () => void
}

const MIN_HEIGHT = 38
const MAX_HEIGHT = 140 // Giới hạn tối đa khoảng 5-6 dòng văn bản

const isImageFile = (file: globalThis.File) => {
  if (file.type?.startsWith('image/')) return true
  return /\.(png|jpe?g|gif|webp|svg|bmp)$/i.test(file.name)
}

interface AttachmentThumbnailProps {
  file: globalThis.File
  onPreviewImage: (url: string, name: string) => void
}

const AttachmentThumbnail = ({ file, onPreviewImage }: AttachmentThumbnailProps) => {
  const isImage = isImageFile(file)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)

  useEffect(() => {
    if (!isImage) return

    let isMounted = true
    const reader = new FileReader()

    reader.onload = () => {
      if (isMounted && typeof reader.result === 'string') {
        setPreviewUrl(reader.result)
      }
    }

    reader.readAsDataURL(file)

    return () => {
      isMounted = false
    }
  }, [file, isImage])

  if (isImage && previewUrl) {
    return (
      <button
        type='button'
        onClick={() => onPreviewImage(previewUrl, file.name)}
        className='relative group h-10 w-10 shrink-0 overflow-hidden rounded-md border border-border/60 cursor-pointer focus:outline-hidden focus:ring-1 focus:ring-ring'
        title='Nhấp để xem to ảnh'
        aria-label={`Xem to ảnh ${file.name}`}
      >
        <img
          src={previewUrl}
          alt={file.name}
          className='h-full w-full object-cover transition-transform group-hover:scale-105'
        />
        <div className='absolute inset-0 bg-black/25 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity'>
          <Maximize2 className='h-3.5 w-3.5 text-white drop-shadow-sm' />
        </div>
      </button>
    )
  }

  return <FileIcon className='h-4 w-4 shrink-0 text-muted-foreground' />
}

export const ChatComposer = ({
  disabled = false,
  placeholder = 'Nhập tin nhắn...',
  onSendMessage,
  onAttachFile,
  onTyping,
  onStopTyping
}: ChatComposerProps) => {
  const [content, setContent] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const { attachments, uploadedFiles, hasUploading, hasFailed, addFiles, retry, remove, clear } = useFileAttachments()

  const [lightboxState, setLightboxState] = useState<{
    isOpen: boolean
    images: LightboxImage[]
    initialIndex: number
  }>({
    isOpen: false,
    images: [],
    initialIndex: 0
  })

  useEffect(() => {
    const textarea = textareaRef.current
    if (!textarea) return

    // Đặt về auto trước để tính scrollHeight chính xác khi người dùng xóa bớt chữ
    textarea.style.height = 'auto'
    const scrollHeight = textarea.scrollHeight

    if (scrollHeight > MAX_HEIGHT) {
      textarea.style.height = `${MAX_HEIGHT}px`
      textarea.style.overflowY = 'auto'
    } else {
      textarea.style.height = `${Math.max(MIN_HEIGHT, scrollHeight)}px`
      textarea.style.overflowY = 'hidden'
    }
  }, [content])

  const handleContentChange = (value: string) => {
    setContent(value)
    if (value.trim().length > 0) {
      onTyping?.()
    } else {
      onStopTyping?.()
    }
  }

  const handleSend = () => {
    const trimmed = content.trim()
    if ((!trimmed && uploadedFiles.length === 0) || disabled || hasUploading || hasFailed) return
    const sent = onSendMessage?.({ content: trimmed, attachments: uploadedFiles })
    if (sent === false) return
    onStopTyping?.()
    setContent('')
    clear()
    if (textareaRef.current) {
      textareaRef.current.style.height = `${MIN_HEIGHT}px`
      textareaRef.current.style.overflowY = 'hidden'
    }
  }

  const handleSelectFiles = (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? [])
    if (files.length > 0) addFiles(files)
    event.target.value = ''
  }

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    handleSend()
  }

  const handlePreviewImage = (url: string, name: string) => {
    setLightboxState({
      isOpen: true,
      images: [{ id: url, name, url }],
      initialIndex: 0
    })
  }

  const isSendDisabled = disabled || hasUploading || hasFailed || (!content.trim() && uploadedFiles.length === 0)

  return (
    <form onSubmit={handleSubmit} className='p-3 border-t bg-card/60 backdrop-blur shrink-0'>
      <input ref={fileInputRef} type='file' multiple className='hidden' onChange={handleSelectFiles} />
      {attachments.length > 0 && (
        <div className='mb-2 space-y-1.5' aria-live='polite'>
          {attachments.map((attachment) => (
            <div
              key={attachment.localId}
              className='flex items-center gap-2.5 rounded-md border bg-background p-1.5 text-xs'
            >
              <AttachmentThumbnail file={attachment.source} onPreviewImage={handlePreviewImage} />
              <div className='min-w-0 flex-1'>
                <p className='truncate font-medium text-foreground'>{attachment.source.name}</p>
                <p className='text-[10px] text-muted-foreground'>{(attachment.source.size / 1024).toFixed(0)} KB</p>
              </div>
              {attachment.status === 'uploading' && (
                <span className='flex items-center gap-1 text-muted-foreground text-[11px] pr-1'>
                  <LoaderCircle className='h-3.5 w-3.5 animate-spin' />
                  {attachment.progress}%
                </span>
              )}
              {attachment.status === 'failed' && (
                <span
                  className='flex min-w-0 items-center gap-1 text-destructive text-[11px] pr-1'
                  title={attachment.error}
                >
                  <AlertCircle className='h-3.5 w-3.5 shrink-0' />
                  <span className='truncate'>{attachment.error ?? 'Lỗi tải tệp'}</span>
                </span>
              )}
              {attachment.status === 'failed' && attachment.canRetry !== false && (
                <Button
                  type='button'
                  variant='ghost'
                  size='icon'
                  className='h-7 w-7'
                  onClick={() => retry(attachment.localId)}
                  aria-label={`Tải lại ${attachment.source.name}`}
                >
                  <RotateCcw className='h-3.5 w-3.5' />
                </Button>
              )}
              <Button
                type='button'
                variant='ghost'
                size='icon'
                className='h-7 w-7'
                onClick={() => remove(attachment.localId)}
                aria-label={`Xóa ${attachment.source.name}`}
              >
                <X className='h-3.5 w-3.5' />
              </Button>
            </div>
          ))}
          {hasFailed && <p className='text-[10px] text-destructive'>Xóa hoặc tải lại tệp lỗi trước khi gửi tin.</p>}
        </div>
      )}
      <div className='flex items-end gap-2'>
        <Button
          type='button'
          variant='ghost'
          size='icon'
          onClick={() => {
            onAttachFile?.()
            fileInputRef.current?.click()
          }}
          disabled={disabled}
          className='h-9 w-9 shrink-0 text-muted-foreground hover:text-foreground mb-0.5'
          title='Đính kèm tệp'
          aria-label='Đính kèm tệp'
        >
          <Paperclip className='h-4 w-4' />
        </Button>

        <div className='flex-1 min-w-0'>
          <Textarea
            ref={textareaRef}
            value={content}
            onChange={(e) => handleContentChange(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={disabled}
            placeholder={placeholder}
            rows={1}
            className='py-2 text-xs leading-normal'
          />
        </div>

        <Button
          type='submit'
          size='icon'
          disabled={isSendDisabled}
          className='h-9 w-9 shrink-0 shadow-sm mb-0.5'
          title='Gửi tin nhắn (Enter)'
          aria-label='Gửi tin nhắn'
        >
          <SendHorizontal className='h-4 w-4' />
        </Button>
      </div>
      <div className='flex justify-between items-center mt-1 px-1'>
        <span className='text-[10px] text-muted-foreground'>
          Nhấn <kbd className='px-1 py-0.5 rounded bg-muted font-mono text-[9px]'>Enter</kbd> để gửi,{' '}
          <kbd className='px-1 py-0.5 rounded bg-muted font-mono text-[9px]'>Shift + Enter</kbd> xuống dòng
        </span>
      </div>

      {/* Lightbox preview for attachments before sending */}
      <ImageLightbox
        isOpen={lightboxState.isOpen}
        images={lightboxState.images}
        initialIndex={lightboxState.initialIndex}
        onClose={() => setLightboxState((prev) => ({ ...prev, isOpen: false }))}
      />
    </form>
  )
}
