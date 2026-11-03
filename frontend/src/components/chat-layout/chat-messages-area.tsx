import { MessageSquareDashed, Users, User as UserIcon } from 'lucide-react'
import { useEffect, useLayoutEffect, useMemo, useRef, useState, type UIEvent } from 'react'
import { Avatar, AvatarFallback, AvatarImage } from '~/components/ui/avatar'
import { Button } from '~/components/ui/button'
import { ImageLightbox, type LightboxImage } from '~/components/chat-layout/image-lightbox'
import { TypingIndicator } from '~/components/chat-layout/typing-indicator'
import { useMarkConversationReadMutation } from '~/hooks/use-conversation'
import { mergeChronologicalMessages, useMessageHistoryQuery } from '~/hooks/use-message-history'
import { getApiErrorMessage } from '~/lib/api-error'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { Message } from '~/types/message.type'
import type { MessageDeliveryState } from '~/lib/message-send'

const TIME_GAP_THRESHOLD_MS = 5 * 60 * 1000 // 5 phút gom cụm tin nhắn

const isImageAttachment = (file: { contentType?: string; name: string }) => {
  if (file.contentType?.startsWith('image/')) return true
  return /\.(png|jpe?g|gif|webp|svg|bmp)$/i.test(file.name)
}

const isDifferentDay = (d1: string | Date, d2: string | Date) => {
  const date1 = new Date(d1)
  const date2 = new Date(d2)
  return (
    date1.getFullYear() !== date2.getFullYear() ||
    date1.getMonth() !== date2.getMonth() ||
    date1.getDate() !== date2.getDate()
  )
}

const formatDateDivider = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const isToday =
    date.getDate() === now.getDate() && date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear()

  const yesterday = new Date()
  yesterday.setDate(now.getDate() - 1)
  const isYesterday =
    date.getDate() === yesterday.getDate() &&
    date.getMonth() === yesterday.getMonth() &&
    date.getFullYear() === yesterday.getFullYear()

  if (isToday) return 'Hôm nay'
  if (isYesterday) return 'Hôm qua'

  return date.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

interface ChatMessagesAreaProps {
  conversationId: string
  title: string
  isGroup?: boolean
  isLoading?: boolean
  optimisticMessages?: Message[]
  loadHistory?: boolean
  messageDeliveryStates?: Record<string, MessageDeliveryState>
  onRetryMessage?: (clientMessageId: string) => void
  typingText?: string
  isTyping?: boolean
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
  messageDeliveryStates = {},
  onRetryMessage,
  typingText,
  isTyping
}: ChatMessagesAreaProps) => {
  const currentUserId = useAuthStore((state) => state.user?.id)
  const history = useMessageHistoryQuery(conversationId, loadHistory && !isLoading)
  const [earlyMessages, setEarlyMessages] = useState<Message[]>([])
  const [lightboxState, setLightboxState] = useState<{
    isOpen: boolean
    images: LightboxImage[]
    initialIndex: number
  }>({
    isOpen: false,
    images: [],
    initialIndex: 0
  })

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

  // Đánh dấu đã đọc tự động khi có tin nhắn thực tế mới nhất
  const markAsReadMutation = useMarkConversationReadMutation()
  const lastMarkedReadIdRef = useRef<string | null>(null)

  const lastRealMessage = useMemo(() => {
    return [...messages].reverse().find((m) => m.id && !m.id.startsWith('pending:'))
  }, [messages])

  useEffect(() => {
    lastMarkedReadIdRef.current = null
  }, [conversationId])

  useEffect(() => {
    if (!conversationId || !lastRealMessage?.id) return
    if (lastMarkedReadIdRef.current === lastRealMessage.id) return
    if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return

    lastMarkedReadIdRef.current = lastRealMessage.id
    markAsReadMutation.mutate({
      conversationId,
      lastReadMessageId: lastRealMessage.id
    })
  }, [conversationId, lastRealMessage?.id, markAsReadMutation])

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && conversationId && lastRealMessage?.id) {
        if (lastMarkedReadIdRef.current !== lastRealMessage.id) {
          lastMarkedReadIdRef.current = lastRealMessage.id
          markAsReadMutation.mutate({
            conversationId,
            lastReadMessageId: lastRealMessage.id
          })
        }
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [conversationId, lastRealMessage?.id, markAsReadMutation])

  const handleOpenLightbox = (images: LightboxImage[], index: number) => {
    setLightboxState({
      isOpen: true,
      images,
      initialIndex: index
    })
  }

  const handleCloseLightbox = () => {
    setLightboxState((prev) => ({ ...prev, isOpen: false }))
  }

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
  }, [history.isSuccess, loadHistory, pageCount, newestMessageId, isTyping])

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
        <div className='flex min-h-full flex-col justify-end'>
          {history.hasNextPage && (
            <div className='mb-3 flex justify-center'>
              <Button variant='ghost' size='sm' onClick={loadOlder} disabled={history.isFetchingNextPage}>
                {history.isFetchingNextPage ? 'Đang tải tin cũ...' : 'Tải tin cũ hơn'}
              </Button>
            </div>
          )}

          {history.isFetchNextPageError && (
            <div className='mb-3 flex flex-col items-center gap-2 text-xs text-destructive' role='alert'>
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

          {messages.map((message, index) => {
            const prevMessage = messages[index - 1]
            const isNewDay = !prevMessage || isDifferentDay(prevMessage.createdAt, message.createdAt)

            const isOwn = Boolean(
              currentUserId && (message.senderId === currentUserId || message.sender?.id === currentUserId)
            )
            const isPrevOwn = Boolean(
              prevMessage &&
              currentUserId &&
              (prevMessage.senderId === currentUserId || prevMessage.sender?.id === currentUserId)
            )
            const getSenderId = (m?: Message) => m?.senderId || m?.sender?.id || ''
            const isSameSender = Boolean(prevMessage && getSenderId(prevMessage) === getSenderId(message))
            const isWithin5Minutes = Boolean(
              prevMessage &&
              Math.abs(new Date(message.createdAt).getTime() - new Date(prevMessage.createdAt).getTime()) <=
                TIME_GAP_THRESHOLD_MS
            )
            // 1. Cùng người gửi <= 5 phút -> gom nhóm
            // 2. > 5 phút -> tách nhóm
            // 3. Người gửi đổi -> tách nhóm
            // 4. Sang ngày mới -> tách nhóm
            const isGroupedWithPrev = isSameSender && isOwn === isPrevOwn && isWithin5Minutes && !isNewDay

            const deliveryState = messageDeliveryStates[message.clientMessageId]
            const sender = message.sender
            const senderName = sender?.fullName || sender?.username || (isGroup ? 'Thành viên' : title)
            const avatarUrl = sender?.avatarUrl || (sender?.avatarFileId ? `/api/files/${sender.avatarFileId}` : null)
            const initials = (senderName.trim().slice(0, 1) || 'U').toUpperCase()

            const attachments = message.attachments || []
            const imageAttachments = attachments.filter(isImageAttachment)
            const otherAttachments = attachments.filter((f) => !isImageAttachment(f))
            const lightboxImages: LightboxImage[] = imageAttachments.map((img) => ({
              id: img.id,
              name: img.name,
              url: img.downloadUrl
            }))

            return (
              <div key={message.id}>
                {isNewDay && (
                  <div className='my-3.5 flex justify-center'>
                    <span className='rounded-full bg-muted/80 px-3 py-1 text-[11px] font-medium text-muted-foreground shadow-xs'>
                      {formatDateDivider(message.createdAt)}
                    </span>
                  </div>
                )}
                <div
                  className={`flex items-start gap-2.5 ${isOwn ? 'justify-end' : 'justify-start'} ${
                    isGroupedWithPrev ? 'mt-1' : isNewDay ? 'mt-1' : index === 0 ? 'mt-0' : 'mt-3.5'
                  }`}
                >
                  {!isOwn &&
                    (!isGroupedWithPrev ? (
                      <Avatar className='mt-0.5 h-8 w-8 shrink-0 border border-border/40 shadow-xs'>
                        {avatarUrl && <AvatarImage src={avatarUrl} alt={senderName} />}
                        <AvatarFallback className='bg-muted text-[11px] font-medium text-muted-foreground'>
                          {initials}
                        </AvatarFallback>
                      </Avatar>
                    ) : (
                      <div className='w-8 shrink-0' aria-hidden='true' />
                    ))}
                  <div
                    className={`flex flex-col min-w-0 ${isOwn ? 'items-end' : 'items-start'} ${
                      imageAttachments.length > 0
                        ? 'max-w-[92%] sm:max-w-[85%] md:max-w-[560px]'
                        : 'max-w-[85%] sm:max-w-[70%]'
                    }`}
                  >
                    {!isOwn && !isGroupedWithPrev && (
                      <span className='mb-1 px-1 text-xs font-medium text-muted-foreground'>{senderName}</span>
                    )}
                    <div
                      className={`rounded-2xl shadow-sm text-sm min-w-0 ${
                        imageAttachments.length > 0 && !message.content && otherAttachments.length === 0
                          ? 'p-1.5'
                          : 'px-3 py-2'
                      } ${isOwn ? 'bg-primary text-primary-foreground' : 'bg-card text-card-foreground'}`}
                    >
                      {/* Image attachments rendered on top (Zalo Style) */}
                      {imageAttachments.length > 0 && (
                        <div
                          className={`overflow-hidden ${
                            imageAttachments.length === 1
                              ? 'max-w-full sm:max-w-[520px]'
                              : imageAttachments.length === 2
                                ? 'grid grid-cols-2 gap-2 max-w-full sm:max-w-[520px]'
                                : 'grid grid-cols-2 sm:grid-cols-3 gap-2 max-w-full sm:max-w-[520px]'
                          }`}
                        >
                          {imageAttachments.map((img, imgIdx) => (
                            <button
                              key={img.id}
                              type='button'
                              onClick={() => handleOpenLightbox(lightboxImages, imgIdx)}
                              className='relative overflow-hidden rounded-xl border border-border/20 bg-muted/40 transition-opacity hover:opacity-95 focus:outline-hidden focus:ring-2 focus:ring-ring group cursor-pointer'
                              title={`Xem ảnh ${img.name}`}
                              aria-label={`Xem ảnh ${img.name}`}
                            >
                              <img
                                src={img.downloadUrl}
                                alt={img.name}
                                loading='lazy'
                                className={`w-full object-cover transition-transform duration-200 group-hover:scale-[1.01] ${
                                  imageAttachments.length === 1
                                    ? 'max-h-[520px] rounded-xl'
                                    : 'aspect-square rounded-lg'
                                }`}
                              />
                            </button>
                          ))}
                        </div>
                      )}

                      {/* Non-image attachments (documents, videos, etc.) rendered above text */}
                      {otherAttachments.length > 0 && (
                        <div className={`space-y-1 text-xs ${imageAttachments.length > 0 ? 'mt-2' : ''}`}>
                          {otherAttachments.map((file) => (
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

                      {/* Text content rendered right below attachments in the same bubble */}
                      {message.content && (
                        <p
                          className={`whitespace-pre-wrap break-words [overflow-wrap:anywhere] min-w-0 ${
                            imageAttachments.length > 0 || otherAttachments.length > 0 ? 'mt-2 px-0.5' : ''
                          }`}
                        >
                          {message.content}
                        </p>
                      )}

                      <time
                        className={`mt-1 block text-right text-[10px] ${
                          isOwn ? 'text-primary-foreground/70' : 'text-muted-foreground'
                        }`}
                        dateTime={message.createdAt}
                      >
                        {new Date(message.createdAt).toLocaleTimeString('vi-VN', {
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </time>
                      {deliveryState?.status === 'pending' && (
                        <div className='mt-1 text-right text-[10px] opacity-70'>Đang gửi...</div>
                      )}
                      {deliveryState?.status === 'failed' && (
                        <div className='mt-1 flex items-center justify-end gap-2 text-[10px] text-destructive'>
                          <span>{deliveryState.error || 'Không gửi được tin nhắn.'}</span>
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
                </div>
              </div>
            )
          })}

          {/* Typing Indicator */}
          {Boolean(isTyping && typingText) && (
            <div className='mt-2'>
              <TypingIndicator text={typingText || ''} visible={Boolean(isTyping && typingText)} />
            </div>
          )}

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

      {/* Lightbox for full-screen image preview (Facebook style) */}
      <ImageLightbox
        isOpen={lightboxState.isOpen}
        images={lightboxState.images}
        initialIndex={lightboxState.initialIndex}
        onClose={handleCloseLightbox}
      />
    </div>
  )
}
