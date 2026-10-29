import { MessageSquarePlus } from 'lucide-react'
import { ChatComposer, ChatHeader, ChatMessagesArea } from '~/components/chat-layout'
import type { Message } from '~/types/message.type'
import type { MessageSendDraft } from '~/lib/message-send'
import type { User } from '~/types/user.type'

interface DirectConversationDraftProps {
  recipientId: string
  recipient?: User
  onBack?: () => void
  onSendMessage?: (draft: MessageSendDraft) => boolean | void
  optimisticMessages?: Message[]
  failedClientMessageIds?: string[]
  onRetryMessage?: (clientMessageId: string) => void
}

export const DirectConversationDraft = ({
  recipientId,
  recipient,
  onBack,
  onSendMessage,
  optimisticMessages,
  failedClientMessageIds,
  onRetryMessage
}: DirectConversationDraftProps) => {
  const title = recipient?.fullName || recipient?.username || 'Hội thoại trực tiếp'
  const subtitle = recipient?.username ? `@${recipient.username}` : `Người nhận #${recipientId.slice(0, 8)}`

  return (
    <div className='flex h-full w-full flex-col overflow-hidden bg-background'>
      <ChatHeader title={title} subtitle={subtitle} onBack={onBack} />
      {optimisticMessages?.length ? (
        <ChatMessagesArea
          conversationId={`draft:${recipientId}`}
          title={title}
          optimisticMessages={optimisticMessages}
          loadHistory={false}
          failedClientMessageIds={failedClientMessageIds}
          onRetryMessage={onRetryMessage}
        />
      ) : (
        <div className='flex min-h-0 flex-1 flex-col items-center justify-center p-6 text-center'>
          <div className='mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10 text-primary'>
            <MessageSquarePlus className='h-7 w-7' aria-hidden='true' />
          </div>
          <h2 className='text-sm font-semibold'>Bắt đầu hội thoại với {title}</h2>
          <p className='mt-1.5 max-w-sm text-xs leading-relaxed text-muted-foreground'>
            Hội thoại sẽ được tạo khi gửi tin nhắn đầu tiên.
          </p>
        </div>
      )}
      <ChatComposer onSendMessage={onSendMessage} placeholder={`Nhắn tin tới ${title}...`} />
    </div>
  )
}
