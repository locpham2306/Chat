import { Users, User as UserIcon } from 'lucide-react'
import { Avatar, AvatarFallback } from '~/components/ui/avatar'
import { Badge } from '~/components/ui/badge'
import { cn } from '~/lib/utils'
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
  const isGroup = conversation.type === 'GROUP'
  const title = isGroup
    ? conversation.name || 'Nhóm chat'
    : conversation.name ||
      conversation.lastMessage?.sender?.fullName ||
      ('title' in conversation && typeof conversation.title === 'string' && conversation.title) ||
      'Hội thoại trực tiếp'
  const time = formatMessageTime(conversation.lastMessage?.createdAt || conversation.createdAt)

  const previewText = conversation.lastMessage
    ? conversation.lastMessage.content || (conversation.lastMessage.attachments?.length ? '[Tệp đính kèm]' : '...')
    : 'Chưa có tin nhắn'

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
      <Avatar className='h-10 w-10 shrink-0'>
        <AvatarFallback className={isGroup ? 'bg-primary/10 text-primary' : undefined}>
          {isGroup ? <Users className='h-4 w-4' /> : <UserIcon className='h-4 w-4' />}
        </AvatarFallback>
      </Avatar>

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
