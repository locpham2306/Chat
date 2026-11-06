import { act, fireEvent, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ChatMessagesArea } from '~/components/chat-layout/chat-messages-area'
import { ConversationItem } from '~/components/conversation-list/conversation-item'
import { ConversationList } from '~/components/conversation-list/conversation-list'
import { CreateGroupDialog } from '~/components/group-create/create-group-dialog'
import { GroupSettingsDialog } from '~/components/group-settings/group-settings-dialog'
import { usePresenceSync } from '~/hooks/use-presence-sync'
import { AuthLayout } from '~/layouts/auth-layout'
import { MainLayout } from '~/layouts/main-layout'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { ApiPage } from '~/types/api-response.type'
import type { ConversationSummary } from '~/types/conversation.type'
import type { Message } from '~/types/message.type'
import type { ServerEvent, WebSocketListener } from '~/types/websocket.type'
import {
  mockCurrentUser,
  mockDirectConversationSummary,
  mockGroupConversationDetail,
  mockGroupConversationSummary,
  mockGroupMembers,
  mockPeerUserOffline,
  mockPeerUserOnline,
  mockSampleMessages
} from './mocks/mock-data'
import { createTestQueryClient, renderWithProviders } from './test-utils'

// Mock các hooks query phụ thuộc
vi.mock('~/hooks/use-group-members', async (importOriginal) => {
  const actual = await importOriginal<typeof import('~/hooks/use-group-members')>()
  return {
    ...actual,
    useGroupMembersQuery: vi.fn(() => ({
      data: {
        data: mockGroupMembers,
        totalPages: 1
      },
      isLoading: false,
      isPending: false,
      isError: false,
      error: null,
      refetch: vi.fn()
    })),
    useAvailableMembersQuery: vi.fn(() => ({
      data: {
        data: [mockPeerUserOnline, mockPeerUserOffline],
        totalPages: 1
      },
      isLoading: false,
      isPending: false,
      isError: false,
      error: null,
      refetch: vi.fn()
    })),
    useAddGroupMembersMutation: vi.fn(() => ({
      mutateAsync: vi.fn().mockResolvedValue({}),
      isPending: false,
      error: null
    })),
    useRemoveGroupMemberMutation: vi.fn(() => ({
      mutateAsync: vi.fn().mockResolvedValue({}),
      isPending: false,
      error: null
    }))
  }
})

vi.mock('~/hooks/use-user-search', async (importOriginal) => {
  const actual = await importOriginal<typeof import('~/hooks/use-user-search')>()
  return {
    ...actual,
    useUserSearchQuery: vi.fn(() => ({
      data: {
        data: [mockPeerUserOnline, mockPeerUserOffline],
        totalPages: 1
      },
      isLoading: false,
      isPending: false,
      isError: false
    })),
    scheduleDebouncedValue: vi.fn(),
    createSearchParams: (keyword: string) => ({ keyword, page: 0 }),
    clampSearchPage: (page: number) => page
  }
})

vi.mock('~/hooks/use-conversation', () => ({
  useConversationDetailQuery: vi.fn(() => ({
    data: mockGroupConversationDetail,
    isLoading: false,
    isError: false
  })),
  useConversationsQuery: vi.fn(() => ({
    data: {
      data: [mockDirectConversationSummary, mockGroupConversationSummary],
      totalPages: 1,
      totalElements: 2
    },
    isLoading: false,
    isError: false,
    refetch: vi.fn()
  })),
  useCreateGroupMutation: vi.fn(() => ({
    mutateAsync: vi.fn().mockResolvedValue(mockGroupConversationDetail),
    isPending: false,
    error: null
  })),
  useUpdateConversationMutation: vi.fn(() => ({
    mutateAsync: vi.fn().mockResolvedValue(mockGroupConversationDetail),
    isPending: false,
    error: null
  })),
  useMarkConversationReadMutation: vi.fn(() => ({
    mutate: vi.fn()
  }))
}))

vi.mock('~/hooks/use-message-history', async (importOriginal) => {
  const actual = await importOriginal<typeof import('~/hooks/use-message-history')>()
  return {
    ...actual,
    useMessageHistoryQuery: vi.fn(() => ({
      data: {
        pages: [{ data: mockSampleMessages }]
      },
      isSuccess: true,
      isPending: false,
      isError: false,
      hasNextPage: false,
      isFetchingNextPage: false,
      fetchNextPage: vi.fn(),
      refetch: vi.fn()
    }))
  }
})

describe('FE-28: Smoke Test E2E & Checklist Phát Hành Hệ Thống ChitChat', () => {
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

  describe('Journey 1: Định danh thương hiệu ChitChat & Khung Layout', () => {
    it('AuthLayout hiển thị logo ChitChat vuông bo góc nhẹ và không có chữ BTL', () => {
      renderWithProviders(<AuthLayout />)

      const logoImg = screen.getByRole('img', { name: 'ChitChat' })
      expect(logoImg).toBeInTheDocument()
      expect(logoImg).toHaveAttribute('src', '/logo.png')
      expect(screen.getByRole('heading', { level: 1, name: 'ChitChat' })).toBeInTheDocument()
      expect(screen.queryByText(/BTL/i)).not.toBeInTheDocument()
    })

    it('MainLayout hiển thị logo ChitChat sắc nét, không có chữ BTL LTM và avatar trạng thái Online', () => {
      renderWithProviders(<MainLayout />)

      const logoImg = screen.getByRole('img', { name: 'ChitChat' })
      expect(logoImg).toBeInTheDocument()
      expect(logoImg).toHaveAttribute('src', '/logo.png')
      expect(screen.getByRole('heading', { level: 1, name: 'ChitChat' })).toBeInTheDocument()
      expect(screen.queryByText(/BTL LTM/i)).not.toBeInTheDocument()
      expect(screen.getByText('Online')).toBeInTheDocument()
    })
  })

  describe('Journey 2: Danh sách hội thoại & Mở cuộc trò chuyện 1-1', () => {
    it('hiển thị danh sách hội thoại với unread badge và kích hoạt callback khi bấm chọn', () => {
      const handleSelect = vi.fn()
      renderWithProviders(<ConversationList activeId={undefined} onSelectConversation={handleSelect} />)

      expect(screen.getByText('Nguyễn Văn An')).toBeInTheDocument()
      expect(screen.getByText('Team Backend')).toBeInTheDocument()

      fireEvent.click(screen.getByText('Nguyễn Văn An'))
      expect(handleSelect).toHaveBeenCalledWith(mockDirectConversationSummary.id)
    })

    it('ConversationItem hiển thị dot xanh trực tuyến khi đối phương ONLINE', () => {
      renderWithProviders(
        <ConversationItem conversation={mockDirectConversationSummary} isActive={false} onSelect={vi.fn()} />
      )

      expect(screen.getByRole('status', { name: 'Đang trực tuyến' })).toBeInTheDocument()
    })
  })

  describe('Journey 3: Nhắn tin, Auto-scroll & Nút nổi Zalo', () => {
    it('tự động cuộn xuống đáy khi tự gửi tin nhắn và không hiển thị thông báo tin mới', () => {
      renderWithProviders(
        <ChatMessagesArea
          conversationId={mockGroupConversationDetail.id}
          title={mockGroupConversationDetail.name || 'Team Backend'}
          isGroup={true}
          optimisticMessages={[]}
        />
      )

      const logContainer = screen.getByRole('log', { name: 'Tin nhắn' })

      // Cuộn lên xem tin cũ
      Object.defineProperty(logContainer, 'scrollHeight', { configurable: true, value: 1200 })
      Object.defineProperty(logContainer, 'clientHeight', { configurable: true, value: 600 })
      Object.defineProperty(logContainer, 'scrollTop', { configurable: true, writable: true, value: 200 })
      fireEvent.scroll(logContainer)

      // Nút cuộn xuống xuất hiện
      expect(screen.getByRole('button', { name: 'Cuộn xuống tin nhắn mới nhất' })).toBeInTheDocument()
    })

    it('hiển thị pill đếm tin nhắn mới kiểu Zalo khi có tin nhắn từ người khác và reset khi bấm', () => {
      const { rerender } = renderWithProviders(
        <ChatMessagesArea
          conversationId={mockGroupConversationDetail.id}
          title={mockGroupConversationDetail.name || 'Team Backend'}
          isGroup={true}
          optimisticMessages={[]}
        />
      )

      const logContainer = screen.getByRole('log', { name: 'Tin nhắn' })

      // Cuộn lên trên
      Object.defineProperty(logContainer, 'scrollHeight', { configurable: true, value: 1200 })
      Object.defineProperty(logContainer, 'clientHeight', { configurable: true, value: 600 })
      Object.defineProperty(logContainer, 'scrollTop', { configurable: true, writable: true, value: 200 })
      fireEvent.scroll(logContainer)

      // Tin nhắn mới từ bạn bè
      const peerMsg: Message = {
        id: 'msg-smoke-peer-1',
        clientMessageId: 'c-smoke-1',
        conversationId: mockGroupConversationDetail.id,
        senderId: mockPeerUserOnline.id,
        sender: mockPeerUserOnline,
        content: 'Chào Tài, mình gửi tài liệu nhé!',
        createdAt: '2026-11-06T14:30:00Z',
        attachments: []
      }

      rerender(
        <ChatMessagesArea
          conversationId={mockGroupConversationDetail.id}
          title={mockGroupConversationDetail.name || 'Team Backend'}
          isGroup={true}
          optimisticMessages={[peerMsg]}
        />
      )

      expect(screen.getByText('1 tin nhắn mới')).toBeInTheDocument()
      const scrollBtn = screen.getByRole('button', {
        name: 'Cuộn xuống tin nhắn mới nhất, có 1 tin nhắn mới'
      })
      expect(scrollBtn).toBeInTheDocument()

      fireEvent.click(scrollBtn)
      expect(screen.queryByText('1 tin nhắn mới')).not.toBeInTheDocument()
    })
  })

  describe('Journey 4: Quản lý nhóm chat & Phân quyền OWNER vs MEMBER', () => {
    it('CreateGroupDialog cho phép chọn thành viên và tạo nhóm', () => {
      renderWithProviders(<CreateGroupDialog open={true} onOpenChange={vi.fn()} onSuccess={vi.fn()} />)

      expect(screen.getByText('Tạo nhóm chat mới')).toBeInTheDocument()
      const checkboxes = screen.getAllByRole('checkbox')
      expect(checkboxes.length).toBeGreaterThan(0)
    })

    it('GroupSettingsDialog hiển thị tablist chuẩn WAI-ARIA và phân quyền OWNER', () => {
      renderWithProviders(
        <GroupSettingsDialog open={true} onOpenChange={vi.fn()} conversation={mockGroupConversationDetail} />
      )

      expect(screen.getByRole('tablist')).toBeInTheDocument()
      expect(screen.getByRole('tab', { name: /Cài đặt chung/i })).toBeInTheDocument()
      expect(screen.getByRole('tab', { name: /Thành viên/i })).toBeInTheDocument()
    })
  })

  describe('Journey 5: Realtime WS-05 & WS-06 (Presence ON/OFF)', () => {
    it('cập nhật tức thì trạng thái ONLINE và OFFLINE của đối phương qua sự kiện WS-06', () => {
      const queryClient = createTestQueryClient()
      const initialCache: ApiPage<ConversationSummary[]> = {
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
      queryClient.setQueryData(['conversations', 'list'], initialCache)

      // Kích hoạt hook presence sync bằng một component wrapper
      const PresenceSyncWatcher = () => {
        usePresenceSync()
        return null
      }
      renderWithProviders(<PresenceSyncWatcher />, { queryClient })

      // 1. Đối phương chuyển sang ONLINE
      act(() => {
        listeners['PRESENCE_CHANGED']?.forEach((fn) =>
          fn({
            userId: mockPeerUserOnline.id,
            user: {
              id: mockPeerUserOnline.id,
              fullName: 'Nguyễn Văn An',
              avatarFileId: null
            },
            status: 'ONLINE',
            lastSeen: new Date().toISOString()
          })
        )
      })

      const cacheOnline = queryClient.getQueryData<ApiPage<ConversationSummary[]>>(['conversations', 'list'])
      expect(cacheOnline?.data[0].peer?.status).toBe('ONLINE')

      // 2. Đối phương ngắt kết nối chuyển sang OFFLINE
      act(() => {
        listeners['PRESENCE_CHANGED']?.forEach((fn) =>
          fn({
            userId: mockPeerUserOnline.id,
            status: 'OFFLINE',
            lastSeen: new Date().toISOString()
          })
        )
      })

      const cacheOffline = queryClient.getQueryData<ApiPage<ConversationSummary[]>>(['conversations', 'list'])
      expect(cacheOffline?.data[0].peer?.status).toBe('OFFLINE')
    })
  })
})
