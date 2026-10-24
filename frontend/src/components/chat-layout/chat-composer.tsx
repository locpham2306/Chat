import { useState, useRef, useEffect, type KeyboardEvent, type FormEvent } from 'react'
import { Paperclip, SendHorizontal } from 'lucide-react'
import { Button } from '~/components/ui/button'
import { Textarea } from '~/components/ui/textarea'

interface ChatComposerProps {
  disabled?: boolean
  placeholder?: string
  onSendMessage?: (content: string) => void
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
    if (!trimmed || disabled) return
    onSendMessage?.(trimmed)
    setContent('')
    if (textareaRef.current) {
      textareaRef.current.style.height = `${MIN_HEIGHT}px`
      textareaRef.current.style.overflowY = 'hidden'
    }
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

  const isSendDisabled = disabled || !content.trim()

  return (
    <form onSubmit={handleSubmit} className='p-3 border-t bg-card/60 backdrop-blur shrink-0'>
      <div className='flex items-end gap-2'>
        <Button
          type='button'
          variant='ghost'
          size='icon'
          onClick={onAttachFile}
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
