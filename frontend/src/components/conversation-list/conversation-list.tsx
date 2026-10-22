import { useState } from 'react'
import { Plus, ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '~/components/ui/button'
import { ScrollArea } from '~/components/ui/scroll-area'
import { useConversationsQuery } from '~/hooks/use-conversation'
import { ConversationEmpty } from './conversation-empty'
import { ConversationError } from './conversation-error'
import { ConversationItem } from './conversation-item'
import { ConversationSkeleton } from './conversation-skeleton'
import type { ConversationSummary } from '~/types/conversation.type'

interface ConversationListProps {
  activeId?: string
  onSelectConversation: (id: string) => void
  onOpenCreateGroup?: () => void
}

export const ConversationList = ({ activeId, onSelectConversation, onOpenCreateGroup }: ConversationListProps) => {
  const [page, setPage] = useState(0)
  const pageSize = 20

  const { data, isLoading, isError, refetch } = useConversationsQuery({
    page,
    size: pageSize
  })

  const items: ConversationSummary[] = Array.isArray(data?.items) ? data.items : []
  const totalElements = data?.totalElements ?? items.length
  const totalPages = data?.totalPages ?? (items.length > 0 ? 1 : 0)

  return (
    <div className='flex flex-col h-full w-full bg-card'>
      {/* Header */}
      <div className='p-3 border-b flex items-center justify-between shrink-0'>
        <div>
          <h2 className='text-sm font-semibold'>Đoạn chat</h2>
          <p className='text-[11px] text-muted-foreground'>
            {isLoading ? 'Đang tải...' : `${totalElements} cuộc trò chuyện`}
          </p>
        </div>
        <Button
          variant='ghost'
          size='icon'
          className='h-8 w-8'
          onClick={onOpenCreateGroup}
          title='Tạo nhóm mới'
          aria-label='Tạo nhóm mới'
        >
          <Plus className='h-4 w-4' />
        </Button>
      </div>

      {/* Main List Area */}
      <div className='flex-1 overflow-hidden relative'>
        {isLoading ? (
          <ConversationSkeleton />
        ) : isError ? (
          <ConversationError onRetry={() => refetch()} />
        ) : items.length === 0 ? (
          <ConversationEmpty />
        ) : (
          <ScrollArea className='h-full w-full'>
            <div className='p-2 space-y-1 w-full min-w-0'>
              {items.map((conversation) => (
                <ConversationItem
                  key={conversation.id}
                  conversation={conversation}
                  isActive={conversation.id === activeId}
                  onSelect={onSelectConversation}
                />
              ))}
            </div>
          </ScrollArea>
        )}
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className='p-2 border-t flex items-center justify-between text-xs text-muted-foreground shrink-0 bg-card'>
          <Button
            variant='ghost'
            size='sm'
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className='h-7 px-2 text-xs gap-1'
          >
            <ChevronLeft className='h-3.5 w-3.5' />
            Trước
          </Button>
          <span>
            {page + 1} / {totalPages}
          </span>
          <Button
            variant='ghost'
            size='sm'
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
            className='h-7 px-2 text-xs gap-1'
          >
            Sau
            <ChevronRight className='h-3.5 w-3.5' />
          </Button>
        </div>
      )}
    </div>
  )
}
