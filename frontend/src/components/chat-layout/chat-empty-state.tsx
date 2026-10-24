import { MessageSquare } from 'lucide-react'

export const ChatEmptyState = () => {
  return (
    <div className='flex-1 flex flex-col items-center justify-center p-6 text-center text-muted-foreground bg-background'>
      <div className='h-16 w-16 rounded-3xl bg-muted/70 text-muted-foreground flex items-center justify-center mb-4 shadow-sm'>
        <MessageSquare className='h-8 w-8 text-primary/70' />
      </div>
      <h2 className='text-base font-semibold text-foreground'>Chưa chọn cuộc trò chuyện</h2>
      <p className='text-xs text-muted-foreground mt-1.5 max-w-sm leading-relaxed'>
        Chọn một cuộc trò chuyện ở danh sách bên trái hoặc bắt đầu tin nhắn mới để trao đổi.
      </p>
    </div>
  )
}
