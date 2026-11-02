import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useRef, useState } from 'react'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { ApiPage } from '~/types/api-response.type'
import type { ConversationDetail, ConversationMember } from '~/types/conversation.type'
import type { NewMessagePayload, ServerTypingPayload } from '~/types/websocket.type'

const THROTTLE_INTERVAL_MS = 2000
const STOP_DEBOUNCE_MS = 2500
const AUTO_DISMISS_MS = 4500

interface UseTypingIndicatorOptions {
  conversationId?: string
  conversation?: ConversationDetail
}

export interface TypingUser {
  userId: string
  name: string
}

export const useTypingIndicator = ({ conversationId, conversation }: UseTypingIndicatorOptions) => {
  const currentUserId = useAuthStore((state) => state.user?.id)
  const queryClient = useQueryClient()

  // Danh sách các userId đang nhập: Map<userId, { timer: NodeJS.Timeout }>
  const [typingMap, setTypingMap] = useState<Map<string, { timer: ReturnType<typeof setTimeout> }>>(new Map())

  // Ref lưu trạng thái gửi typing của client hiện tại
  const isSelfTypingRef = useRef(false)
  const lastSentTimeRef = useRef(0)
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const currentConvIdRef = useRef<string | undefined>(conversationId)

  useEffect(() => {
    currentConvIdRef.current = conversationId
  }, [conversationId])

  // Dừng typing và gửi isTyping: false
  const stopTyping = useCallback(() => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
      debounceTimerRef.current = null
    }

    const activeId = currentConvIdRef.current
    if (isSelfTypingRef.current && activeId) {
      isSelfTypingRef.current = false
      lastSentTimeRef.current = 0
      wsClient.send('TYPING', {
        conversationId: activeId,
        isTyping: false
      })
    }
  }, [])

  // Kích hoạt khi người dùng gõ phím
  const triggerTyping = useCallback(() => {
    const activeId = currentConvIdRef.current
    // RÀNG BUỘC BẮT BUỘC: Chỉ gửi khi đã có conversationId
    if (!activeId) return

    const now = Date.now()

    // 1. Throttle gửi isTyping: true
    if (!isSelfTypingRef.current || now - lastSentTimeRef.current >= THROTTLE_INTERVAL_MS) {
      isSelfTypingRef.current = true
      lastSentTimeRef.current = now
      wsClient.send('TYPING', {
        conversationId: activeId,
        isTyping: true
      })
    }

    // 2. Debounce tự động gửi isTyping: false khi ngừng gõ
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }

    debounceTimerRef.current = setTimeout(() => {
      stopTyping()
    }, STOP_DEBOUNCE_MS)
  }, [stopTyping])

  // Khi unmount hoặc đổi conversation, gửi isTyping: false cho cuộc trò chuyện cũ
  useEffect(() => {
    return () => {
      stopTyping()
    }
  }, [conversationId, stopTyping])

  // Hàm tìm tên hiển thị của userId
  const resolveUserName = useCallback(
    (userId: string): string => {
      if (!conversation) return 'Ai đó'

      // Nếu là hội thoại 1-1 và đúng là peer
      if (conversation.type === 'DIRECT' && conversation.peer?.id === userId) {
        return conversation.peer.fullName || conversation.peer.username || 'Người dùng'
      }

      // Nếu là group, thử tìm trong cache thành viên nhóm
      if (conversationId) {
        const queries = queryClient.getQueriesData<ApiPage<ConversationMember[]>>({
          queryKey: ['group-members', conversationId]
        })
        for (const [, pageData] of queries) {
          const found = pageData?.data?.find((m: ConversationMember) => m.user.id === userId)
          if (found?.user) {
            return found.user.fullName || found.user.username || 'Thành viên'
          }
        }
      }

      return 'Ai đó'
    },
    [conversation, conversationId, queryClient]
  )

  // Lắng nghe sự kiện TYPING từ WebSocket
  useEffect(() => {
    if (!conversationId) return

    const handleTypingEvent = (payload: ServerTypingPayload) => {
      // Chỉ xử lý cho hội thoại hiện tại và không phải chính mình
      if (payload.conversationId !== conversationId || payload.userId === currentUserId) {
        return
      }

      setTypingMap((prev) => {
        const next = new Map(prev)
        const existing = next.get(payload.userId)

        if (existing) {
          clearTimeout(existing.timer)
        }

        if (payload.isTyping) {
          // Tạo timeout tự động xóa sau AUTO_DISMISS_MS
          const timer = setTimeout(() => {
            setTypingMap((current) => {
              const updated = new Map(current)
              updated.delete(payload.userId)
              return updated
            })
          }, AUTO_DISMISS_MS)

          next.set(payload.userId, { timer })
        } else {
          next.delete(payload.userId)
        }

        return next
      })
    }

    // Khi người gửi gửi tin nhắn mới đến, xóa ngay chỉ báo typing của họ
    const handleNewMessage = (message: NewMessagePayload) => {
      if (message.conversationId !== conversationId) return
      const senderId = message.senderId || message.sender?.id
      if (!senderId) return

      setTypingMap((prev) => {
        if (!prev.has(senderId)) return prev
        const next = new Map(prev)
        const item = next.get(senderId)
        if (item) clearTimeout(item.timer)
        next.delete(senderId)
        return next
      })
    }

    const unsubTyping = wsClient.on('TYPING', handleTypingEvent)
    const unsubNewMessage = wsClient.on('NEW_MESSAGE', handleNewMessage)

    return () => {
      unsubTyping()
      unsubNewMessage()
      setTypingMap((current) => {
        current.forEach((item) => clearTimeout(item.timer))
        return new Map()
      })
    }
  }, [conversationId, currentUserId])

  // Xây dựng danh sách typingUsers và chuỗi typingText
  const userIds = conversationId ? Array.from(typingMap.keys()) : []
  const typingUsers: TypingUser[] = userIds.map((userId) => ({
    userId,
    name: resolveUserName(userId)
  }))

  let typingText = ''
  if (typingUsers.length === 1) {
    typingText = `${typingUsers[0].name} đang nhập...`
  } else if (typingUsers.length === 2) {
    typingText = `${typingUsers[0].name} và ${typingUsers[1].name} đang nhập...`
  } else if (typingUsers.length > 2) {
    typingText = `${typingUsers[0].name} và ${typingUsers.length - 1} người khác đang nhập...`
  }

  return {
    typingUsers,
    isTyping: typingUsers.length > 0,
    typingText,
    triggerTyping,
    stopTyping
  }
}
