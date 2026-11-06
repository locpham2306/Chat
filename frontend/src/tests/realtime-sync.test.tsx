import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePresenceSync } from '~/hooks/use-presence-sync'
import { useRealtimeMessageSync } from '~/hooks/use-realtime-message-sync'
import { useTypingIndicator } from '~/hooks/use-typing-indicator'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { ApiPage } from '~/types/api-response.type'
import type { ConversationDetail, ConversationSummary } from '~/types/conversation.type'
import type { Message } from '~/types/message.type'
import type { ServerEvent, WebSocketListener } from '~/types/websocket.type'
import {
  mockCurrentUser,
  mockDirectConversationDetail,
  mockDirectConversationSummary,
  mockGroupConversationDetail,
  mockPeerUserOffline,
  mockPeerUserOnline
} from './mocks/mock-data'
import { createTestQueryClient } from './test-utils'

const createWrapper = (queryClient: QueryClient) => {
  return function HookWrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

describe('FE-12, FE-20 & FE-22: Realtime WebSocket, Typing, Presence & Read State', () => {
  const listeners: { [key: string]: ((payload: any) => void)[] } = {}
  const statusListeners: ((status: any) => void)[] = []

  beforeEach(() => {
    Object.keys(listeners).forEach((k) => delete listeners[k])
    statusListeners.length = 0
    useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

    vi.spyOn(wsClient, 'on').mockImplementation((event: ServerEvent, handler: WebSocketListener<any>) => {
      if (!listeners[event]) {
        listeners[event] = []
      }
      listeners[event].push(handler)
      return () => {
        listeners[event] = listeners[event]?.filter((h) => h !== handler)
      }
    })

    vi.spyOn(wsClient, 'onStatusChange').mockImplementation((handler) => {
      statusListeners.push(handler)
      return () => {
        const index = statusListeners.indexOf(handler)
        if (index !== -1) statusListeners.splice(index, 1)
      }
    })

    vi.spyOn(wsClient, 'send').mockImplementation(() => true)
  })

  describe('FE-20: Chỉ báo đang nhập (WS-04 TYPING)', () => {
    it('hiển thị tên người đang gõ khi nhận sự kiện TYPING và tự ẩn khi isTyping = false', () => {
      const queryClient = createTestQueryClient()
      const { result } = renderHook(
        () =>
          useTypingIndicator({
            conversationId: 'conv-direct-0001',
            conversation: mockDirectConversationDetail
          }),
        { wrapper: createWrapper(queryClient) }
      )

      expect(result.current.isTyping).toBe(false)

      // Giả lập Server gửi WS-04 TYPING { userId: peerId, isTyping: true }
      act(() => {
        listeners['TYPING']?.forEach((fn) =>
          fn({
            conversationId: 'conv-direct-0001',
            userId: mockPeerUserOnline.id,
            isTyping: true
          })
        )
      })

      expect(result.current.isTyping).toBe(true)
      expect(result.current.typingText).toContain('Nguyễn Văn An đang nhập')

      // Giả lập Server gửi WS-04 TYPING { userId: peerId, isTyping: false }
      act(() => {
        listeners['TYPING']?.forEach((fn) =>
          fn({
            conversationId: 'conv-direct-0001',
            userId: mockPeerUserOnline.id,
            isTyping: false
          })
        )
      })

      expect(result.current.isTyping).toBe(false)
    })

    it('hiển thị chính xác tên người đang gõ trong nhóm chat từ thông tin user trong payload TYPING thay vì hiển thị Ai đó', () => {
      const queryClient = createTestQueryClient()
      const { result } = renderHook(
        () =>
          useTypingIndicator({
            conversationId: mockGroupConversationDetail.id,
            conversation: mockGroupConversationDetail
          }),
        { wrapper: createWrapper(queryClient) }
      )

      expect(result.current.isTyping).toBe(false)

      // Giả lập Server gửi sự kiện TYPING trong nhóm chat kèm thông tin user (Trần Gia Bình)
      act(() => {
        listeners['TYPING']?.forEach((fn) =>
          fn({
            conversationId: mockGroupConversationDetail.id,
            userId: 'user-binh-id',
            user: {
              id: 'user-binh-id',
              fullName: 'Trần Gia Bình',
              avatarFileId: null
            },
            isTyping: true
          })
        )
      })

      expect(result.current.isTyping).toBe(true)
      expect(result.current.typingText).toBe('Trần Gia Bình đang nhập...')
      expect(result.current.typingText).not.toContain('Ai đó')
      expect(result.current.typingUsers[0].name).toBe('Trần Gia Bình')
    })

    it('tự động ẩn chỉ báo đang nhập khi người đó gửi tin nhắn mới (NEW_MESSAGE)', () => {
      const queryClient = createTestQueryClient()
      const { result } = renderHook(
        () =>
          useTypingIndicator({
            conversationId: 'conv-direct-0001',
            conversation: mockDirectConversationDetail
          }),
        { wrapper: createWrapper(queryClient) }
      )

      // Đang nhập...
      act(() => {
        listeners['TYPING']?.forEach((fn) =>
          fn({
            conversationId: 'conv-direct-0001',
            userId: mockPeerUserOnline.id,
            isTyping: true
          })
        )
      })
      expect(result.current.isTyping).toBe(true)

      // Tin nhắn mới đến từ chính người đó
      act(() => {
        listeners['NEW_MESSAGE']?.forEach((fn) =>
          fn({
            id: 'msg-new-001',
            conversationId: 'conv-direct-0001',
            senderId: mockPeerUserOnline.id,
            sender: mockPeerUserOnline,
            clientMessageId: 'c-new-001',
            content: 'Xong rồi nè',
            attachments: [],
            createdAt: new Date().toISOString()
          })
        )
      })

      // Đang nhập tự động biến mất
      expect(result.current.isTyping).toBe(false)
    })
  })

  describe('FE-22: Trạng thái Online/Offline Realtime (WS-06 PRESENCE_CHANGED)', () => {
    it('cập nhật tức thì trạng thái ONLINE trong cache khi đối phương đăng nhập mà không cần F5', () => {
      const queryClient = createTestQueryClient()

      // Khởi tạo cache với đối tác đang OFFLINE
      const initialConversations: ApiPage<ConversationSummary[]> = {
        data: [
          {
            ...mockDirectConversationSummary,
            peer: {
              ...mockDirectConversationSummary.peer!,
              status: 'OFFLINE'
            }
          }
        ],
        totalPages: 1,
        totalElements: 1
      }
      queryClient.setQueryData(['conversations', 'list'], initialConversations)

      renderHook(() => usePresenceSync({ activeConversationId: 'conv-direct-0001' }), {
        wrapper: createWrapper(queryClient)
      })

      // Giả lập WS-06 PRESENCE_CHANGED phát sự kiện bạn bè ONLINE
      act(() => {
        listeners['PRESENCE_CHANGED']?.forEach((fn) =>
          fn({
            userId: mockPeerUserOnline.id,
            status: 'ONLINE',
            lastSeen: new Date().toISOString()
          })
        )
      })

      // Kiểm tra cache đã được cập nhật thành ONLINE mà không cần gọi API
      const updatedCache = queryClient.getQueryData<ApiPage<ConversationSummary[]>>(['conversations', 'list'])
      expect(updatedCache?.data[0].peer?.status).toBe('ONLINE')
    })

    it('cập nhật tức thì trạng thái OFFLINE khi đối phương ngắt kết nối', () => {
      const queryClient = createTestQueryClient()

      const initialDetail: ConversationDetail = {
        ...mockDirectConversationDetail,
        peer: {
          ...mockDirectConversationDetail.peer!,
          status: 'ONLINE'
        }
      }
      queryClient.setQueryData(['conversations', 'detail', 'conv-direct-0001'], initialDetail)

      renderHook(() => usePresenceSync({ activeConversationId: 'conv-direct-0001' }), {
        wrapper: createWrapper(queryClient)
      })

      // Giả lập WS-06 PRESENCE_CHANGED phát sự kiện bạn bè OFFLINE
      act(() => {
        listeners['PRESENCE_CHANGED']?.forEach((fn) =>
          fn({
            userId: mockPeerUserOnline.id,
            status: 'OFFLINE',
            lastSeen: '2026-11-03T11:00:00Z'
          })
        )
      })

      const updatedDetail = queryClient.getQueryData<ConversationDetail>([
        'conversations',
        'detail',
        'conv-direct-0001'
      ])
      expect(updatedDetail?.peer?.status).toBe('OFFLINE')
    })

    it('cập nhật đồng thời thông tin user (tên, avatar) và trạng thái từ payload WS-06', () => {
      const queryClient = createTestQueryClient()

      const initialConversations: ApiPage<ConversationSummary[]> = {
        data: [
          {
            ...mockDirectConversationSummary,
            peer: {
              ...mockDirectConversationSummary.peer!,
              fullName: 'Tên Cũ',
              avatarFileId: null,
              status: 'OFFLINE'
            }
          }
        ],
        totalPages: 1,
        totalElements: 1
      }
      queryClient.setQueryData(['conversations', 'list'], initialConversations)

      renderHook(() => usePresenceSync({ activeConversationId: 'conv-direct-0001' }), {
        wrapper: createWrapper(queryClient)
      })

      // Giả lập WS-06 PRESENCE_CHANGED có chứa thông tin user mới
      act(() => {
        listeners['PRESENCE_CHANGED']?.forEach((fn) =>
          fn({
            userId: mockPeerUserOnline.id,
            user: {
              id: mockPeerUserOnline.id,
              fullName: 'Tên Mới Sau Khi Cập Nhật',
              avatarFileId: 'avatar-new-123'
            },
            status: 'ONLINE',
            lastSeen: new Date().toISOString()
          })
        )
      })

      const updatedCache = queryClient.getQueryData<ApiPage<ConversationSummary[]>>(['conversations', 'list'])
      expect(updatedCache?.data[0].peer?.status).toBe('ONLINE')
      expect(updatedCache?.data[0].peer?.fullName).toBe('Tên Mới Sau Khi Cập Nhật')
      expect(updatedCache?.data[0].peer?.avatarFileId).toBe('avatar-new-123')
    })

    it('đồng bộ trạng thái Online/Offline của chính người dùng hiện tại qua WebSocket', () => {
      const queryClient = createTestQueryClient()
      useAuthStore.setState({ user: { ...mockCurrentUser, status: 'OFFLINE' }, isAuthenticated: true })

      renderHook(() => usePresenceSync(), {
        wrapper: createWrapper(queryClient)
      })

      // Giả lập WS-06 gửi thông báo user hiện tại chuyển ONLINE
      act(() => {
        listeners['PRESENCE_CHANGED']?.forEach((fn) =>
          fn({
            userId: mockCurrentUser.id,
            status: 'ONLINE',
            lastSeen: new Date().toISOString()
          })
        )
      })

      expect(useAuthStore.getState().user?.status).toBe('ONLINE')

      // Giả lập kết nối WebSocket bị đóng
      act(() => {
        statusListeners.forEach((fn) => fn('CLOSED'))
      })

      expect(useAuthStore.getState().user?.status).toBe('OFFLINE')
    })
  })

  describe('FE-22 & FE-12: Mốc đã đọc & Đồng bộ unreadCount', () => {
    it('xóa unreadCount về 0 khi nhận WS-05 READ_RECEIPT của chính mình', () => {
      const queryClient = createTestQueryClient()

      const initialConversations: ApiPage<ConversationSummary[]> = {
        data: [
          {
            ...mockDirectConversationSummary,
            unreadCount: 5
          }
        ],
        totalPages: 1,
        totalElements: 1
      }
      queryClient.setQueryData(['conversations', 'list'], initialConversations)

      renderHook(() => usePresenceSync({ activeConversationId: 'conv-direct-0001' }), {
        wrapper: createWrapper(queryClient)
      })

      // Giả lập WS-05 READ_RECEIPT do chính người dùng đọc
      act(() => {
        listeners['READ_RECEIPT']?.forEach((fn) =>
          fn({
            conversationId: mockDirectConversationSummary.id,
            userId: mockCurrentUser.id,
            lastReadMessageId: 'msg-001'
          })
        )
      })

      const cache = queryClient.getQueryData<ApiPage<ConversationSummary[]>>(['conversations', 'list'])
      expect(cache?.data[0].unreadCount).toBe(0)
    })

    it('giữ unreadCount = 0 khi nhận tin nhắn thuộc cuộc trò chuyện đang mở trực tiếp', () => {
      const queryClient = createTestQueryClient()

      const initialConversations: ApiPage<ConversationSummary[]> = {
        data: [
          {
            ...mockDirectConversationSummary,
            unreadCount: 0
          }
        ],
        totalPages: 1,
        totalElements: 1
      }
      queryClient.setQueryData(['conversations', 'list'], initialConversations)

      renderHook(
        () =>
          useRealtimeMessageSync({
            activeConversationId: 'conv-direct-0001'
          }),
        { wrapper: createWrapper(queryClient) }
      )

      const incomingMsg: Message = {
        id: 'msg-new-999',
        conversationId: 'conv-direct-0001',
        clientMessageId: 'c-999',
        content: 'Tin nhắn khi đang mở chat',
        senderId: mockPeerUserOnline.id,
        sender: mockPeerUserOnline,
        attachments: [],
        createdAt: new Date().toISOString()
      }

      act(() => {
        listeners['NEW_MESSAGE']?.forEach((fn) => fn(incomingMsg))
      })

      const cache = queryClient.getQueryData<ApiPage<ConversationSummary[]>>(['conversations', 'list'])
      expect(cache?.data[0].unreadCount).toBe(0)
    })

    it('tăng unreadCount khi nhận tin nhắn từ cuộc trò chuyện khác', () => {
      const queryClient = createTestQueryClient()

      const initialConversations: ApiPage<ConversationSummary[]> = {
        data: [
          {
            ...mockDirectConversationSummary,
            id: 'conv-other-0003',
            unreadCount: 1
          }
        ],
        totalPages: 1,
        totalElements: 1
      }
      queryClient.setQueryData(['conversations', 'list'], initialConversations)

      renderHook(
        () =>
          useRealtimeMessageSync({
            activeConversationId: 'conv-direct-0001'
          }),
        { wrapper: createWrapper(queryClient) }
      )

      const incomingMsg: Message = {
        id: 'msg-new-888',
        conversationId: 'conv-other-0003',
        clientMessageId: 'c-888',
        content: 'Tin nhắn từ cuộc trò chuyện khác',
        senderId: mockPeerUserOffline.id,
        sender: mockPeerUserOffline,
        attachments: [],
        createdAt: new Date().toISOString()
      }

      act(() => {
        listeners['NEW_MESSAGE']?.forEach((fn) => fn(incomingMsg))
      })

      const cache = queryClient.getQueryData<ApiPage<ConversationSummary[]>>(['conversations', 'list'])
      expect(cache?.data[0].unreadCount).toBe(2)
    })
  })
})
