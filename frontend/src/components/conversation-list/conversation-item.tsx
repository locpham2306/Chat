import { Users, User as UserIcon } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from '~/components/ui/avatar'
import { Badge } from '~/components/ui/badge'
import { cn } from '~/lib/utils'
import { useAuthStore } from '~/stores/use-auth-store'
import type { ConversationSummary } from '~/types/conversation.type'

interface ConversationItemProps {
  conversation: ConversationSummary
  isActive?: boolean
  onSelect: (id: string) => void
}

const formatMessageTime = (dateString?: string | null) => {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    const now = new Date()
    const isToday =
      date.getDate() === now.getDate() && date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear()

    if (isToday) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
    return date.toLocaleDateString([], { month: '2-digit', day: '2-digit' })
  } catch {
    return ''
  }
}

export const ConversationItem = ({ conversation, isActive = false, onSelect }: ConversationItemProps) => {
  const currentUser = useAuthStore((state) => state.user)
  const isGroup = conversation.type === 'GROUP'
  const peer = conversation.peer
  const lastMsg = conversation.lastMessage
  const currentUserId = currentUser?.id

  const isMe = Boolean(
    (lastMsg?.sender?.id && lastMsg.sender.id === currentUserId) ||
    (lastMsg?.senderId && lastMsg.senderId === currentUserId)
  )

  const otherSenderName = !isMe && lastMsg?.sender?.fullName ? lastMsg.sender.fullName : null

  const title = isGroup
    ? conversation.name || 'Nhóm chat'
    : peer?.fullName ||
      ('title' in conversation && typeof conversation.title === 'string' && conversation.title) ||
      otherSenderName ||
      'Hội thoại trực tiếp'

  const avatarFileId = isGroup ? conversation.avatarFileId : peer?.avatarFileId
  const avatarUrl = avatarFileId ? `/api/files/${avatarFileId}` : null
  const isOnline = !isGroup && peer?.status === 'ONLINE'

  const time = formatMessageTime(lastMsg?.createdAt || conversation.createdAt)

  const getPreviewText = () => {
    if (!lastMsg) return 'Chưa có tin nhắn'

    const senderName = isMe ? 'Bạn' : lastMsg.sender?.fullName || ''
    const contentText =
      lastMsg.content?.trim() || (lastMsg.attachments && lastMsg.attachments.length > 0 ? '[Tệp đính kèm]' : '...')

    if (!senderName) return contentText
    return `${senderName}: ${contentText}`
  }

  const previewText = getPreviewText()

  return (
    <div
      role='button'
      tabIndex={0}
      onClick={() => onSelect(conversation.id)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          onSelect(conversation.id)
        }
      }}
      className={cn(
        'flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-colors text-left border border-transparent select-none w-full min-w-0',
        isActive ? 'bg-accent text-accent-foreground border-border' : 'hover:bg-muted/60 text-foreground'
      )}
    >
      <div className='relative shrink-0'>
        <Avatar className='h-10 w-10'>
          {avatarUrl && <AvatarImage src={avatarUrl} alt={title} />}
          <AvatarFallback className={isGroup ? 'bg-primary/10 text-primary' : undefined}>
            {isGroup ? <Users className='h-4 w-4' /> : <UserIcon className='h-4 w-4' />}
          </AvatarFallback>
        </Avatar>
        {isOnline && (
          <span
            className='absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full bg-emerald-500 ring-2 ring-background'
            title='Đang trực tuyến'
          />
        )}
      </div>

      <div className='flex-1 min-w-0'>
        <div className='flex items-center justify-between gap-1'>
          <p className='text-xs font-semibold truncate leading-tight'>{title}</p>
          {time && <span className='text-[10px] text-muted-foreground shrink-0'>{time}</span>}
        </div>
        <div className='flex items-center justify-between gap-1 mt-1'>
          <p className='text-xs text-muted-foreground truncate leading-tight'>{previewText}</p>
          {conversation.unreadCount > 0 && (
            <Badge variant='destructive' className='h-4 px-1.5 text-[10px] font-bold shrink-0'>
              {conversation.unreadCount}
            </Badge>
          )}
        </div>
      </div>
    </div>
  )
}
