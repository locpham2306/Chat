import { MessageSquareDashed, Users, User as UserIcon } from 'lucide-react'
import { ScrollArea } from '~/components/ui/scroll-area'

interface ChatMessagesAreaProps {
  conversationId: string
  title: string
  isGroup?: boolean
  isLoading?: boolean
}

export const ChatMessagesArea = ({
  conversationId,
  title,
  isGroup = false,
  isLoading = false
}: ChatMessagesAreaProps) => {
  return (
    <div className='flex-1 overflow-hidden relative bg-muted/10'>
      <ScrollArea className='h-full w-full'>
        <div className='flex flex-col justify-end min-h-full p-4 space-y-4'>
          {/* Welcome / Empty state banner */}
          <div className='flex flex-col items-center justify-center py-12 text-center text-muted-foreground my-auto'>
            <div className='h-14 w-14 rounded-full bg-primary/10 text-primary flex items-center justify-center mb-3 shadow-sm'>
              {isGroup ? <Users className='h-7 w-7' /> : <UserIcon className='h-7 w-7' />}
            </div>
            <h3 className='text-sm font-semibold text-foreground'>{title}</h3>
            <p className='text-xs text-muted-foreground mt-1 max-w-sm'>
              {isGroup
                ? 'Đây là khởi đầu của nhóm chat. Mọi tin nhắn sẽ được đồng bộ tới tất cả thành viên.'
                : 'Bắt đầu cuộc trò chuyện. Hãy gửi lời chào đầu tiên để kết nối!'}
            </p>
            <div className='mt-4 flex items-center gap-1.5 text-[11px] text-muted-foreground bg-muted/60 px-3 py-1.5 rounded-full'>
              <MessageSquareDashed className='h-3.5 w-3.5' />
              <span>Hội thoại #{conversationId.slice(0, 8)}</span>
            </div>
          </div>

          {isLoading && (
            <div className='flex items-center justify-center py-4'>
              <span className='text-xs text-muted-foreground animate-pulse'>Đang tải tin nhắn...</span>
            </div>
          )}
        </div>
      </ScrollArea>
    </div>
  )
}
