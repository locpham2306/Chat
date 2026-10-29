import { MessageSquareDashed, Users, User as UserIcon } from 'lucide-react'
import { useEffect, useLayoutEffect, useMemo, useRef, useState, type UIEvent } from 'react'
import { Button } from '~/components/ui/button'
import { mergeChronologicalMessages, useMessageHistoryQuery } from '~/hooks/use-message-history'
import { getApiErrorMessage } from '~/lib/api-error'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { Message } from '~/types/message.type'

interface ChatMessagesAreaProps {
  conversationId: string
  title: string
  isGroup?: boolean
  isLoading?: boolean
  optimisticMessages?: Message[]
  loadHistory?: boolean
  failedClientMessageIds?: string[]
  onRetryMessage?: (clientMessageId: string) => void
}

interface ScrollSnapshot {
  height: number
  top: number
  pageCount: number
}

export const ChatMessagesArea = ({
  conversationId,
  title,
  isGroup = false,
  isLoading = false,
  optimisticMessages = [],
  loadHistory = true,
  failedClientMessageIds = [],
  onRetryMessage
}: ChatMessagesAreaProps) => {
  const currentUserId = useAuthStore((state) => state.user?.id)
  const history = useMessageHistoryQuery(conversationId, loadHistory && !isLoading)
  const [earlyMessages, setEarlyMessages] = useState<Message[]>([])
  const hasHistory = Boolean(history.data)
  const messages = useMemo(
    () =>
      mergeChronologicalMessages([
        { data: optimisticMessages, nextCursor: null, hasMore: false },
        { data: earlyMessages, nextCursor: null, hasMore: false },
        ...(history.data?.pages ?? [])
      ]),
    [earlyMessages, history.data, optimisticMessages]
  )
  const scrollRef = useRef<HTMLDivElement>(null)
  const initializedRef = useRef(false)
  const nearBottomRef = useRef(true)
  const snapshotRef = useRef<ScrollSnapshot | null>(null)
  const pageCount = history.data?.pages.length ?? 0
  const newestMessageId = messages.at(-1)?.id

  useEffect(
    () =>
      wsClient.on('NEW_MESSAGE', (message) => {
        if (message.conversationId === conversationId && !hasHistory) {
          setEarlyMessages((current) => [...current, message])
        }
      }),
    [conversationId, hasHistory]
  )

  useLayoutEffect(() => {
    const viewport = scrollRef.current
    if (!viewport || (loadHistory && !history.isSuccess)) return

    const snapshot = snapshotRef.current
    if (snapshot && pageCount > snapshot.pageCount) {
      viewport.scrollTop = snapshot.top + viewport.scrollHeight - snapshot.height
      snapshotRef.current = null
      return
    }

    if (snapshot) {
      snapshot.height = viewport.scrollHeight
      snapshot.top = viewport.scrollTop
      return
    }

    if (!initializedRef.current || nearBottomRef.current) {
      viewport.scrollTop = viewport.scrollHeight
      initializedRef.current = true
    }
  }, [history.isSuccess, loadHistory, pageCount, newestMessageId])

  const loadOlder = () => {
    const viewport = scrollRef.current
    if (!loadHistory || !viewport || !history.hasNextPage || history.isFetchingNextPage) return

    snapshotRef.current = {
      height: viewport.scrollHeight,
      top: viewport.scrollTop,
      pageCount
    }
    void history.fetchNextPage()
  }

  const handleScroll = (event: UIEvent<HTMLDivElement>) => {
    const viewport = event.currentTarget
    if (snapshotRef.current) snapshotRef.current.top = viewport.scrollTop
    nearBottomRef.current = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight < 80
    if (viewport.scrollTop < 80 && !history.isFetchNextPageError) loadOlder()
  }

  const retryOlder = () => {
    snapshotRef.current = null
    loadOlder()
  }

  const showEmpty = (history.isSuccess || !loadHistory) && messages.length === 0

  return (
    <div className='relative min-h-0 flex-1 bg-muted/10'>
      <div
        ref={scrollRef}
        onScroll={handleScroll}
        className='h-full overflow-y-auto p-4 [overflow-anchor:none]'
        role='log'
        aria-label='Tin nhắn'
        tabIndex={0}
      >
        <div className='flex min-h-full flex-col justify-end gap-3'>
          {history.hasNextPage && (
            <div className='flex justify-center'>
              <Button variant='ghost' size='sm' onClick={loadOlder} disabled={history.isFetchingNextPage}>
                {history.isFetchingNextPage ? 'Đang tải tin cũ...' : 'Tải tin cũ hơn'}
              </Button>
            </div>
          )}

          {history.isFetchNextPageError && (
            <div className='flex flex-col items-center gap-2 text-xs text-destructive' role='alert'>
              <span>{getApiErrorMessage(history.error)}</span>
              <Button variant='outline' size='sm' onClick={retryOlder}>
                Thử lại
              </Button>
            </div>
          )}

          {showEmpty && (
            <div className='my-auto flex flex-col items-center justify-center py-12 text-center text-muted-foreground'>
              <div className='mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-primary/10 text-primary shadow-sm'>
                {isGroup ? <Users className='h-7 w-7' /> : <UserIcon className='h-7 w-7' />}
              </div>
              <h3 className='text-sm font-semibold text-foreground'>{title}</h3>
              <p className='mt-1 max-w-sm text-xs'>Chưa có tin nhắn trong hội thoại này.</p>
              <div className='mt-4 flex items-center gap-1.5 rounded-full bg-muted/60 px-3 py-1.5 text-[11px]'>
                <MessageSquareDashed className='h-3.5 w-3.5' />
                <span>Hội thoại #{conversationId.slice(0, 8)}</span>
              </div>
            </div>
          )}

          {messages.map((message) => {
            const isOwn = Boolean(
              currentUserId && (message.senderId === currentUserId || message.sender?.id === currentUserId)
            )
            const isFailed = failedClientMessageIds.includes(message.clientMessageId)
            return (
              <div key={message.id} className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[85%] rounded-2xl px-3 py-2 text-sm shadow-sm sm:max-w-[70%] ${isOwn ? 'bg-primary text-primary-foreground' : 'bg-card text-card-foreground'}`}
                >
                  {isGroup && !isOwn && message.sender?.fullName && (
                    <p className='mb-1 text-xs font-semibold'>{message.sender.fullName}</p>
                  )}
                  {message.content && <p className='whitespace-pre-wrap break-words'>{message.content}</p>}
                  {message.attachments.length > 0 && (
                    <div className='mt-1 space-y-1 text-xs'>
                      {message.attachments.map((file) => (
                        <a
                          key={file.id}
                          href={file.downloadUrl}
                          download
                          className='block truncate underline-offset-2 hover:underline'
                        >
                          📎 {file.name}
                        </a>
                      ))}
                    </div>
                  )}
                  <time
                    className={`mt-1 block text-right text-[10px] ${isOwn ? 'text-primary-foreground/70' : 'text-muted-foreground'}`}
                    dateTime={message.createdAt}
                  >
                    {new Date(message.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                  </time>
                  {isFailed && (
                    <div className='mt-1 flex items-center justify-end gap-2 text-[10px] text-destructive'>
                      <span>Không gửi được</span>
                      <Button
                        type='button'
                        variant='ghost'
                        size='sm'
                        className='h-auto px-1 py-0 text-[10px] text-inherit hover:text-inherit'
                        onClick={() => onRetryMessage?.(message.clientMessageId)}
                      >
                        Thử lại
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            )
          })}

          {(isLoading || (loadHistory && history.isPending)) && (
            <p className='py-4 text-center text-xs text-muted-foreground' role='status'>
              Đang tải tin nhắn...
            </p>
          )}

          {loadHistory && history.isError && !history.isFetchNextPageError && (
            <div className='flex flex-col items-center gap-2 py-4 text-xs text-destructive' role='alert'>
              <span>{getApiErrorMessage(history.error)}</span>
              <Button variant='outline' size='sm' onClick={() => void history.refetch()}>
                Thử lại
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
