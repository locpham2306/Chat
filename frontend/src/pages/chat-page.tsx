import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, MessageSquare } from 'lucide-react'
import { Button } from '~/components/ui/button'
import { ConversationList } from '~/components/conversation-list/conversation-list'
import { cn } from '~/lib/utils'

export const ChatPage = () => {
  const { conversationId } = useParams()
  const navigate = useNavigate()

  const handleSelectConversation = (id: string) => {
    navigate(`/chat/${id}`)
  }

  const handleBackToList = () => {
    navigate('/chat')
  }

  return (
    <div className='flex h-full w-full overflow-hidden'>
      {/* Sidebar: Danh sách hội thoại */}
      <aside
        className={cn(
          'w-full sm:w-80 border-r bg-card shrink-0 h-full flex flex-col',
          conversationId ? 'hidden sm:flex' : 'flex'
        )}
      >
        <ConversationList activeId={conversationId} onSelectConversation={handleSelectConversation} />
      </aside>

      {/* Vùng chat chính (FE-08 sẽ hoàn thiện chi tiết) */}
      <section
        className={cn(
          'flex-1 flex-col h-full bg-background',
          conversationId ? 'flex' : 'hidden sm:flex items-center justify-center'
        )}
      >
        {conversationId ? (
          <div className='flex flex-col h-full'>
            {/* Header chat mobile back button */}
            <div className='h-14 border-b px-4 flex items-center gap-2 sm:hidden shrink-0'>
              <Button
                variant='ghost'
                size='icon'
                onClick={handleBackToList}
                className='h-8 w-8'
                aria-label='Quay lại danh sách'
              >
                <ArrowLeft className='h-4 w-4' />
              </Button>
              <span className='text-sm font-semibold truncate'>Hội thoại #{conversationId}</span>
            </div>

            <div className='flex-1 flex flex-col items-center justify-center p-6 text-center text-muted-foreground'>
              <div className='h-12 w-12 rounded-2xl bg-muted text-muted-foreground flex items-center justify-center mb-3'>
                <MessageSquare className='h-6 w-6' />
              </div>
              <h2 className='text-base font-semibold text-foreground'>Hội thoại #{conversationId}</h2>
              <p className='text-xs text-muted-foreground mt-1 max-w-sm'>
                Khung hiển thị tin nhắn và composer đang chờ ghép ở Task FE-08.
              </p>
            </div>
          </div>
        ) : (
          <div className='flex flex-col items-center justify-center p-6 text-center text-muted-foreground'>
            <div className='h-12 w-12 rounded-2xl bg-muted text-muted-foreground flex items-center justify-center mb-3'>
              <MessageSquare className='h-6 w-6' />
            </div>
            <h2 className='text-base font-semibold text-foreground'>Chưa chọn cuộc trò chuyện</h2>
            <p className='text-xs text-muted-foreground mt-1 max-w-sm'>
              Chọn một cuộc trò chuyện ở danh sách bên trái hoặc bắt đầu tin nhắn mới.
            </p>
          </div>
        )}
      </section>
    </div>
  )
}
