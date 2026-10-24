import { useNavigate, useParams } from 'react-router-dom'
import { ChatEmptyState, ChatLayout } from '~/components/chat-layout'
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

      {/* Vùng chat chính (FE-08) */}
      <section
        className={cn(
          'flex-1 flex-col h-full bg-background',
          conversationId ? 'flex' : 'hidden sm:flex items-center justify-center'
        )}
      >
        {conversationId ? <ChatLayout conversationId={conversationId} onBack={handleBackToList} /> : <ChatEmptyState />}
      </section>
    </div>
  )
}
