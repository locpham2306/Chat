import axios from 'axios'
import { ShieldAlert } from 'lucide-react'
import { ChatComposer } from './chat-composer'
import { ChatHeader } from './chat-header'
import { ChatMessagesArea } from './chat-messages-area'
import { Button } from '~/components/ui/button'
import { useConversationDetailQuery } from '~/hooks/use-conversation'

interface ChatLayoutProps {
  conversationId: string
  onBack?: () => void
  onSendMessage?: (content: string) => void
  onAttachFile?: () => void
  onOpenInfo?: () => void
}

export const ChatLayout = ({ conversationId, onBack, onSendMessage, onAttachFile, onOpenInfo }: ChatLayoutProps) => {
  const { data: conversation, isLoading, isError, error } = useConversationDetailQuery(conversationId)

  if (isError) {
    const isForbiddenOrNotFound =
      axios.isAxiosError(error) && (error.response?.status === 403 || error.response?.status === 404)

    return (
      <div className='flex flex-col h-full w-full bg-background items-center justify-center p-6 text-center select-none'>
        <div className='mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-destructive/10 text-destructive'>
          <ShieldAlert className='h-7 w-7' aria-hidden='true' />
        </div>
        <h2 className='text-sm font-semibold'>Không thể truy cập hội thoại</h2>
        <p className='mt-1.5 max-w-sm text-xs leading-relaxed text-muted-foreground'>
          {isForbiddenOrNotFound
            ? 'Hội thoại không tồn tại hoặc bạn không có quyền truy cập.'
            : 'Đã xảy ra lỗi khi tải thông tin hội thoại. Vui lòng thử lại sau.'}
        </p>
        {onBack && (
          <Button variant='outline' size='sm' onClick={onBack} className='mt-4 text-xs'>
            Quay lại danh sách
          </Button>
        )}
      </div>
    )
  }

  const isGroup = conversation?.type === 'GROUP'

  let title = `Hội thoại #${conversationId.slice(0, 8)}`
  let subtitle: string | undefined

  if (conversation) {
    if (isGroup) {
      title = conversation.name || 'Nhóm chat'
      subtitle = `${conversation.memberCount} thành viên`
    } else {
      title = conversation.peer?.fullName || conversation.name || 'Hội thoại trực tiếp'
      subtitle = conversation.peer?.status === 'ONLINE' ? 'Trực tuyến' : undefined
    }
  }

  const avatarFileId = isGroup ? conversation?.avatarFileId : conversation?.peer?.avatarFileId
  const avatarUrl = avatarFileId ? `/api/files/${avatarFileId}` : null

  return (
    <div className='flex flex-col h-full w-full bg-background overflow-hidden'>
      {/* 1. Chat Header */}
      <ChatHeader
        title={title}
        isGroup={isGroup}
        subtitle={subtitle}
        avatarUrl={avatarUrl}
        onBack={onBack}
        onOpenInfo={onOpenInfo}
      />

      {/* 2. Messages Area */}
      <ChatMessagesArea
        key={conversationId}
        conversationId={conversationId}
        title={title}
        isGroup={isGroup}
        isLoading={isLoading}
      />

      {/* 3. Chat Composer */}
      <ChatComposer
        onSendMessage={onSendMessage}
        onAttachFile={onAttachFile}
        placeholder={`Nhắn tin tới ${title}...`}
      />
    </div>
  )
}
