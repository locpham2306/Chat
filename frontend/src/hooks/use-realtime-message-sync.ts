import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useRef } from 'react'
import conversationApi from '~/apis/conversation.api'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { ApiPage } from '~/types/api-response.type'
import type { ConversationDetail, ConversationSummary } from '~/types/conversation.type'
import type { Message } from '~/types/message.type'
import type { MessageAckPayload, NewMessagePayload } from '~/types/websocket.type'

export const messageKeys = {
  all: ['messages'] as const,
  list: (conversationId?: string) => ['messages', 'list', conversationId] as const
}

interface UseRealtimeMessageSyncOptions {
  activeConversationId?: string
  onMessageAck?: (payload: MessageAckPayload) => void
  onNewMessage?: (payload: NewMessagePayload) => void
}

/**
 * Cập nhật tin nhắn trong cache của conversationId (hỗ trợ cả Message[], CursorPage, InfiniteData)
 */
function updateMessageCache(
  messages: Message[],
  target: { id?: string; clientMessageId?: string },
  updater: (existing: Message) => Message,
  onNotFoundAppend?: () => Message
): { updatedList: Message[]; found: boolean } {
  let found = false
  const updatedList = messages.map((msg) => {
    const isMatch =
      (target.id && msg.id === target.id) || (target.clientMessageId && msg.clientMessageId === target.clientMessageId)

    if (isMatch) {
      found = true
      return updater(msg)
    }
    return msg
  })

  if (!found && onNotFoundAppend) {
    const newMsg = onNotFoundAppend()
    updatedList.push(newMsg)
    found = true
  }

  return { updatedList, found }
}

export const useRealtimeMessageSync = (options: UseRealtimeMessageSyncOptions = {}) => {
  const { activeConversationId, onMessageAck, onNewMessage } = options
  const queryClient = useQueryClient()
  const currentUser = useAuthStore((state) => state.user)
  const currentUserId = currentUser?.id

  const activeConversationIdRef = useRef(activeConversationId)
  const currentUserIdRef = useRef(currentUserId)
  const onMessageAckRef = useRef(onMessageAck)
  const onNewMessageRef = useRef(onNewMessage)

  useEffect(() => {
    activeConversationIdRef.current = activeConversationId
    currentUserIdRef.current = currentUserId
    onMessageAckRef.current = onMessageAck
    onNewMessageRef.current = onNewMessage
  })

  /**
   * Khử trùng và cập nhật cache tin nhắn
   */
  const handleUpsertMessageInCache = useCallback(
    (conversationId: string, newMessage: Message) => {
      queryClient.setQueryData(messageKeys.list(conversationId), (oldData: unknown) => {
        if (!oldData) return oldData

        // 1. Trường hợp data là Mảng đơn thuần Message[]
        if (Array.isArray(oldData)) {
          const { updatedList } = updateMessageCache(
            oldData,
            { id: newMessage.id, clientMessageId: newMessage.clientMessageId },
            () => newMessage,
            () => newMessage
          )
          return updatedList
        }

        // 2. Trường hợp data dạng Cursor/Page: { data: Message[], nextCursor?: string, hasMore?: boolean }
        if (
          typeof oldData === 'object' &&
          oldData !== null &&
          'data' in oldData &&
          Array.isArray((oldData as { data: unknown }).data)
        ) {
          const pageData = oldData as { data: Message[]; nextCursor?: string | null; hasMore?: boolean }
          const { updatedList } = updateMessageCache(
            pageData.data,
            { id: newMessage.id, clientMessageId: newMessage.clientMessageId },
            () => newMessage,
            () => newMessage
          )
          return { ...pageData, data: updatedList }
        }

        // 3. Trường hợp TanStack Query InfiniteData: { pages: Array<{ data: Message[] }> }
        if (
          typeof oldData === 'object' &&
          oldData !== null &&
          'pages' in oldData &&
          Array.isArray((oldData as { pages: unknown }).pages)
        ) {
          const infiniteData = oldData as { pages: Array<{ data: Message[] }>; pageParams: unknown[] }
          let messageHandled = false

          const newPages = infiniteData.pages.map((page) => {
            if (!Array.isArray(page.data)) return page
            const { updatedList, found } = updateMessageCache(
              page.data,
              { id: newMessage.id, clientMessageId: newMessage.clientMessageId },
              () => newMessage
            )
            if (found) messageHandled = true
            return { ...page, data: updatedList }
          })

          // Nếu chưa có trong bất kỳ page nào, append vào page mới nhất (page cuối hoặc đầu tùy thiết kế)
          if (!messageHandled && newPages.length > 0) {
            const targetPage = newPages[0]
            if (Array.isArray(targetPage.data)) {
              newPages[0] = {
                ...targetPage,
                data: [...targetPage.data, newMessage]
              }
            }
          }

          return { ...infiniteData, pages: newPages }
        }

        return oldData
      })
    },
    [queryClient]
  )

  /**
   * Cập nhật tin nhắn tạm sang tin chính thức khi nhận MESSAGE_ACK
   */
  const handleApplyMessageAckInCache = useCallback(
    (payload: MessageAckPayload) => {
      queryClient.setQueryData(messageKeys.list(payload.conversationId), (oldData: unknown) => {
        if (!oldData) return oldData

        const updateFn = (msg: Message): Message => ({
          ...msg,
          id: payload.messageId,
          createdAt: payload.createdAt || msg.createdAt
        })

        // 1. Trường hợp Mảng Message[]
        if (Array.isArray(oldData)) {
          const { updatedList } = updateMessageCache(
            oldData,
            { clientMessageId: payload.clientMessageId, id: payload.messageId },
            updateFn
          )
          return updatedList
        }

        // 2. Trường hợp Cursor Page
        if (
          typeof oldData === 'object' &&
          oldData !== null &&
          'data' in oldData &&
          Array.isArray((oldData as { data: unknown }).data)
        ) {
          const pageData = oldData as { data: Message[] }
          const { updatedList } = updateMessageCache(
            pageData.data,
            { clientMessageId: payload.clientMessageId, id: payload.messageId },
            updateFn
          )
          return { ...pageData, data: updatedList }
        }

        // 3. Trường hợp InfiniteData
        if (
          typeof oldData === 'object' &&
          oldData !== null &&
          'pages' in oldData &&
          Array.isArray((oldData as { pages: unknown }).pages)
        ) {
          const infiniteData = oldData as { pages: Array<{ data: Message[] }>; pageParams: unknown[] }
          const newPages = infiniteData.pages.map((page) => {
            if (!Array.isArray(page.data)) return page
            const { updatedList } = updateMessageCache(
              page.data,
              { clientMessageId: payload.clientMessageId, id: payload.messageId },
              updateFn
            )
            return { ...page, data: updatedList }
          })
          return { ...infiniteData, pages: newPages }
        }

        return oldData
      })
    },
    [queryClient]
  )

  /**
   * Cập nhật cache danh sách hội thoại khi có tin nhắn mới hoặc ACK
   */
  const handleUpdateConversationList = useCallback(
    async (message: Message) => {
      let isConversationFound = false

      queryClient.setQueriesData<ApiPage<ConversationSummary[]>>({ queryKey: ['conversations', 'list'] }, (old) => {
        if (!old || !Array.isArray(old.data)) return old

        const targetId = message.conversationId
        const existingIndex = old.data.findIndex((c) => c.id === targetId)

        if (existingIndex === -1) {
          return old
        }

        isConversationFound = true
        const targetConv = old.data[existingIndex]
        const isFromOther =
          Boolean(message.senderId && message.senderId !== currentUserIdRef.current) ||
          Boolean(message.sender?.id && message.sender.id !== currentUserIdRef.current)

        const isNotActiveChat = activeConversationIdRef.current !== targetId
        const nextUnreadCount =
          isFromOther && isNotActiveChat ? (targetConv.unreadCount || 0) + 1 : targetConv.unreadCount || 0

        const updatedConv: ConversationSummary = {
          ...targetConv,
          lastMessage: message,
          unreadCount: nextUnreadCount
        }

        // Đưa cuộc trò chuyện lên đầu danh sách (index 0)
        const remaining = old.data.filter((_, idx) => idx !== existingIndex)
        return {
          ...old,
          data: [updatedConv, ...remaining]
        }
      })

      // Nếu conversationId chưa có trong cache danh sách, gọi HTTP-11 để lấy chi tiết và đưa lên đầu
      if (!isConversationFound) {
        try {
          const response = await conversationApi.getConversationDetail(message.conversationId)
          const detail: ConversationDetail = response.data.content

          if (detail && detail.id) {
            const isFromOther =
              Boolean(message.senderId && message.senderId !== currentUserIdRef.current) ||
              Boolean(message.sender?.id && message.sender.id !== currentUserIdRef.current)
            const isNotActiveChat = activeConversationIdRef.current !== detail.id

            const newConvSummary: ConversationSummary =
              detail.type === 'GROUP'
                ? {
                    id: detail.id,
                    type: 'GROUP',
                    name: detail.name || 'Nhóm chat',
                    avatarFileId: detail.avatarFileId,
                    ownerId: detail.ownerId || '',
                    memberCount: detail.memberCount || 2,
                    createdAt: detail.createdAt,
                    lastMessage: message,
                    unreadCount: isFromOther && isNotActiveChat ? 1 : 0
                  }
                : {
                    id: detail.id,
                    type: 'DIRECT',
                    memberIds: detail.peer ? [detail.peer.id] : [],
                    name: detail.name,
                    avatarFileId: detail.avatarFileId,
                    createdAt: detail.createdAt,
                    peer: detail.peer,
                    lastMessage: message,
                    unreadCount: isFromOther && isNotActiveChat ? 1 : 0
                  }

            queryClient.setQueriesData<ApiPage<ConversationSummary[]>>(
              { queryKey: ['conversations', 'list'] },
              (old) => {
                if (!old || !Array.isArray(old.data)) return old
                if (old.data.some((c) => c.id === detail.id)) return old
                return {
                  ...old,
                  data: [newConvSummary, ...old.data],
                  totalElements: (old.totalElements || 0) + 1
                }
              }
            )
          }
        } catch (error) {
          console.warn('[FE-12] Không thể lấy chi tiết cuộc hội thoại mới:', error)
          // Fallback refetch danh sách
          queryClient.invalidateQueries({ queryKey: ['conversations', 'list'] })
        }
      }
    },
    [queryClient]
  )

  /**
   * Lắng nghe các sự kiện WebSocket WS-02 và WS-03
   */
  useEffect(() => {
    // 1. Lắng nghe WS-02 MESSAGE_ACK
    const unsubscribeAck = wsClient.on('MESSAGE_ACK', (payload: MessageAckPayload) => {
      handleApplyMessageAckInCache(payload)

      // Cập nhật lastMessage của hội thoại nếu khớp clientMessageId
      queryClient.setQueriesData<ApiPage<ConversationSummary[]>>({ queryKey: ['conversations', 'list'] }, (old) => {
        if (!old || !Array.isArray(old.data)) return old

        let changed = false
        const updated = old.data.map((c) => {
          if (
            c.id === payload.conversationId &&
            c.lastMessage &&
            c.lastMessage.clientMessageId === payload.clientMessageId
          ) {
            changed = true
            return {
              ...c,
              lastMessage: {
                ...c.lastMessage,
                id: payload.messageId,
                createdAt: payload.createdAt || c.lastMessage.createdAt
              }
            }
          }
          return c
        })

        return changed ? { ...old, data: updated } : old
      })

      onMessageAckRef.current?.(payload)
    })

    // 2. Lắng nghe WS-03 NEW_MESSAGE
    const unsubscribeNewMessage = wsClient.on('NEW_MESSAGE', (payload: NewMessagePayload) => {
      // Khử trùng và đồng bộ vào cache tin nhắn
      handleUpsertMessageInCache(payload.conversationId, payload)

      // Cập nhật và đưa hội thoại lên đầu danh sách
      void handleUpdateConversationList(payload)

      onNewMessageRef.current?.(payload)
    })

    return () => {
      unsubscribeAck()
      unsubscribeNewMessage()
    }
  }, [handleApplyMessageAckInCache, handleUpsertMessageInCache, handleUpdateConversationList, queryClient])
}
