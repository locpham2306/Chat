import { useState, useRef, useEffect, type ChangeEvent, type KeyboardEvent, type FormEvent } from 'react'
import { AlertCircle, FileIcon, LoaderCircle, Paperclip, RotateCcw, SendHorizontal, X } from 'lucide-react'
import { Button } from '~/components/ui/button'
import { Textarea } from '~/components/ui/textarea'
import { useFileAttachments } from '~/hooks/use-file-attachments'
import type { MessageSendDraft } from '~/lib/message-send'

interface ChatComposerProps {
  disabled?: boolean
  placeholder?: string
  onSendMessage?: (draft: MessageSendDraft) => boolean | void
  onAttachFile?: () => void
}

const MIN_HEIGHT = 38
const MAX_HEIGHT = 140 // Giới hạn tối đa khoảng 5-6 dòng văn bản

export const ChatComposer = ({
  disabled = false,
  placeholder = 'Nhập tin nhắn...',
  onSendMessage,
  onAttachFile
}: ChatComposerProps) => {
  const [content, setContent] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const { attachments, uploadedFiles, hasUploading, hasFailed, addFiles, retry, remove, clear } = useFileAttachments()

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

  const handleSend = () => {
    const trimmed = content.trim()
    if ((!trimmed && uploadedFiles.length === 0) || disabled || hasUploading || hasFailed) return
    const sent = onSendMessage?.({ content: trimmed, attachments: uploadedFiles })
    if (sent === false) return
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

  const isSendDisabled = disabled || hasUploading || hasFailed || (!content.trim() && uploadedFiles.length === 0)

  return (
    <form onSubmit={handleSubmit} className='p-3 border-t bg-card/60 backdrop-blur shrink-0'>
      <input ref={fileInputRef} type='file' multiple className='hidden' onChange={handleSelectFiles} />
      {attachments.length > 0 && (
        <div className='mb-2 space-y-1.5' aria-live='polite'>
          {attachments.map((attachment) => (
            <div
              key={attachment.localId}
              className='flex items-center gap-2 rounded-md border bg-background px-2 py-1.5 text-xs'
            >
              <FileIcon className='h-3.5 w-3.5 shrink-0 text-muted-foreground' />
              <span className='min-w-0 flex-1 truncate'>{attachment.source.name}</span>
              {attachment.status === 'uploading' && (
                <span className='flex items-center gap-1 text-muted-foreground'>
                  <LoaderCircle className='h-3 w-3 animate-spin' />
                  {attachment.progress}%
                </span>
              )}
              {attachment.status === 'failed' && (
                <span className='flex min-w-0 items-center gap-1 text-destructive' title={attachment.error}>
                  <AlertCircle className='h-3 w-3' />
                  <span className='truncate'>{attachment.error ?? 'Lỗi tải tệp'}</span>
                </span>
              )}
              {attachment.status === 'failed' && attachment.canRetry !== false && (
                <Button
                  type='button'
                  variant='ghost'
                  size='icon'
                  className='h-6 w-6'
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
                className='h-6 w-6'
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
            onChange={(e) => setContent(e.target.value)}
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
    </form>
  )
}
