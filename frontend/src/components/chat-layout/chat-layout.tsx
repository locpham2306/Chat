import { ChatComposer } from './chat-composer'
import { ChatHeader } from './chat-header'
import { ChatMessagesArea } from './chat-messages-area'
import { useConversationsQuery } from '~/hooks/use-conversation'

interface ChatLayoutProps {
  conversationId: string
  onBack?: () => void
  onSendMessage?: (content: string) => void
  onAttachFile?: () => void
  onOpenInfo?: () => void
}

export const ChatLayout = ({ conversationId, onBack, onSendMessage, onAttachFile, onOpenInfo }: ChatLayoutProps) => {
  const { data, isLoading } = useConversationsQuery()

  const conversation = data?.items?.find((item) => item.id === conversationId)
  const isGroup = conversation?.type === 'GROUP'

  let title = `Hội thoại #${conversationId.slice(0, 8)}`
  if (conversation) {
    if (isGroup) {
      title = conversation.name || 'Nhóm chat'
    } else if ('title' in conversation && typeof conversation.title === 'string' && conversation.title) {
      title = conversation.title
    } else {
      title = 'Hội thoại trực tiếp'
    }
  }

  const subtitle =
    isGroup && conversation ? `${conversation.memberCount} thành viên` : conversation ? 'Trực tuyến' : undefined

  return (
    <div className='flex flex-col h-full w-full bg-background overflow-hidden'>
      {/* 1. Chat Header */}
      <ChatHeader title={title} isGroup={isGroup} subtitle={subtitle} onBack={onBack} onOpenInfo={onOpenInfo} />

      {/* 2. Messages Area */}
      <ChatMessagesArea conversationId={conversationId} title={title} isGroup={isGroup} isLoading={isLoading} />

      {/* 3. Chat Composer */}
      <ChatComposer
        onSendMessage={onSendMessage}
        onAttachFile={onAttachFile}
        placeholder={`Nhắn tin tới ${title}...`}
      />
    </div>
  )
}
