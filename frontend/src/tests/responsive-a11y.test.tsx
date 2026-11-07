import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ChatHeader } from '~/components/chat-layout/chat-header'
import { ChatMessagesArea } from '~/components/chat-layout/chat-messages-area'
import { ConversationItem } from '~/components/conversation-list/conversation-item'
import { ConversationList } from '~/components/conversation-list/conversation-list'
import { CreateGroupDialog } from '~/components/group-create/create-group-dialog'
import { AddMembersDialog } from '~/components/group-settings/add-members-dialog'
import { GroupSettingsDialog } from '~/components/group-settings/group-settings-dialog'
import { useAuthStore } from '~/stores/use-auth-store'
import type { Message } from '~/types/message.type'
import {
  mockCurrentUser,
  mockDirectConversationSummary,
  mockGroupConversationDetail,
  mockGroupConversationSummary,
  mockGroupMembers,
  mockSampleMessages,
  mockPeerUserOffline,
  mockPeerUserOnline
} from './mocks/mock-data'
import { renderWithProviders } from './test-utils'

// Mock hook queries
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

describe('FE-26: Responsive & Accessibility Chat Nhóm & Layout', () => {
  describe('GroupSettingsDialog: WAI-ARIA Tabs & Alert Dialogs', () => {
    it('chuẩn hóa tablist, tab role, aria-selected và điều hướng phím mũi tên', () => {
      useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

      renderWithProviders(
        <GroupSettingsDialog open={true} onOpenChange={vi.fn()} conversation={mockGroupConversationDetail} />
      )

      // Tablist container
      const tabList = screen.getByRole('tablist', { name: /Cài đặt nhóm/ })
      expect(tabList).toBeInTheDocument()

      // Các tab con
      const infoTab = screen.getByRole('tab', { name: /Cài đặt chung/ })
      const membersTab = screen.getByRole('tab', { name: /Thành viên/ })

      expect(infoTab).toHaveAttribute('aria-selected', 'true')
      expect(membersTab).toHaveAttribute('aria-selected', 'false')

      // Điều hướng phím mũi tên: từ infoTab bấm ArrowRight -> chuyển sang membersTab
      fireEvent.keyDown(infoTab, { key: 'ArrowRight' })
      expect(screen.getByRole('tab', { name: /Thành viên/ })).toHaveAttribute('aria-selected', 'true')
      expect(screen.getByRole('tabpanel', { name: /Thành viên/ })).toBeInTheDocument()

      // Bấm ArrowLeft -> chuyển lại về infoTab
      fireEvent.keyDown(membersTab, { key: 'ArrowLeft' })
      expect(screen.getByRole('tab', { name: /Cài đặt chung/ })).toHaveAttribute('aria-selected', 'true')
      expect(screen.getByRole('tabpanel', { name: /Cài đặt chung/ })).toBeInTheDocument()
    })

    it('chuẩn hóa hộp thoại xác nhận xóa thành viên với role alertdialog', () => {
      useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

      renderWithProviders(
        <GroupSettingsDialog open={true} onOpenChange={vi.fn()} conversation={mockGroupConversationDetail} />
      )

      // Chuyển sang tab Thành viên
      fireEvent.click(screen.getByRole('tab', { name: /Thành viên/ }))

      // Bấm nút xóa thành viên Nguyễn Văn An
      const removeBtn = screen.getByLabelText(/Xóa Nguyễn Văn An khỏi nhóm/)
      fireEvent.click(removeBtn)

      // Hộp thoại xác nhận xuất hiện dưới dạng alertdialog
      const alertDialog = screen.getByRole('alertdialog')
      expect(alertDialog).toBeInTheDocument()
      expect(alertDialog.getAttribute('aria-labelledby')).toContain('remove-member-dialog-title')
      expect(alertDialog.getAttribute('aria-describedby')).toContain('remove-member-dialog-desc')
      expect(screen.getByText('Xóa thành viên khỏi nhóm?')).toBeInTheDocument()
    })

    it('hiển thị aria-invalid và thông báo lỗi khi tên nhóm rỗng', () => {
      useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

      renderWithProviders(
        <GroupSettingsDialog open={true} onOpenChange={vi.fn()} conversation={mockGroupConversationDetail} />
      )

      const nameInput = screen.getByLabelText(/Tên nhóm/)
      fireEvent.change(nameInput, { target: { value: '' } })

      expect(nameInput).toHaveAttribute('aria-invalid', 'true')
      expect(nameInput.getAttribute('aria-describedby')).toContain('settings-group-name-error')
      expect(screen.getByRole('alert')).toHaveTextContent('Tên nhóm không được để trống')
    })
  })

  describe('CreateGroupDialog & AddMembersDialog: Checkbox List & Keyboard Selection', () => {
    it('CreateGroupDialog chuẩn hóa danh sách ứng viên với role checkbox và toggle bằng phím Space/Enter', () => {
      renderWithProviders(<CreateGroupDialog open={true} onOpenChange={vi.fn()} />)

      // Ô tìm kiếm có aria-label
      expect(screen.getByLabelText(/Tìm kiếm thành viên/)).toBeInTheDocument()

      // Ứng viên có role checkbox
      const candidateCheckboxes = screen.getAllByRole('checkbox')
      expect(candidateCheckboxes.length).toBeGreaterThan(0)

      const firstCandidate = candidateCheckboxes[0]
      expect(firstCandidate).toHaveAttribute('aria-checked', 'false')

      // Chọn bằng phím Space
      fireEvent.keyDown(firstCandidate, { key: ' ' })
      expect(firstCandidate).toHaveAttribute('aria-checked', 'true')

      // Bỏ chọn bằng phím Enter
      fireEvent.keyDown(firstCandidate, { key: 'Enter' })
      expect(firstCandidate).toHaveAttribute('aria-checked', 'false')
    })

    it('AddMembersDialog chuẩn hóa role checkbox và hỗ trợ aria-label đầy đủ', () => {
      renderWithProviders(<AddMembersDialog open={true} onOpenChange={vi.fn()} conversationId='conv-123' />)

      expect(screen.getByLabelText(/Tìm theo họ tên hoặc username để thêm vào nhóm/)).toBeInTheDocument()

      const candidateCheckboxes = screen.getAllByRole('checkbox')
      expect(candidateCheckboxes.length).toBeGreaterThan(0)
      expect(candidateCheckboxes[0]).toHaveAttribute('aria-checked', 'false')

      fireEvent.click(candidateCheckboxes[0])
      expect(candidateCheckboxes[0]).toHaveAttribute('aria-checked', 'true')
    })
  })

  describe('ConversationList & ConversationItem: a11y & Responsive text truncation', () => {
    it('ConversationList có role region và ConversationItem có aria-current, aria-label', () => {
      renderWithProviders(
        <ConversationList activeId={mockDirectConversationSummary.id} onSelectConversation={vi.fn()} />
      )

      const region = screen.getByRole('region', { name: 'Danh sách cuộc trò chuyện' })
      expect(region).toBeInTheDocument()

      const activeItem = screen.getByLabelText(/Cuộc trò chuyện với Nguyễn Văn An/)
      expect(activeItem).toHaveAttribute('aria-current', 'true')
      expect(activeItem).toBeInTheDocument()

      // Trạng thái trực tuyến có role status và aria-label
      expect(screen.getByLabelText('Đang trực tuyến')).toBeInTheDocument()
    })

    it('ConversationItem xử lý tốt tên dài và văn bản dài mà không vỡ layout', () => {
      const longNameConversation = {
        ...mockGroupConversationSummary,
        id: 'long-group-1',
        name: 'Nhóm Dự Án Lập Trình Mạng Phân Tán Với Tên Cực Kỳ Dài Nhằm Kiểm Thử Độ Co Giãn Layout Mobile',
        unreadCount: 99
      }

      renderWithProviders(<ConversationItem conversation={longNameConversation} isActive={false} onSelect={vi.fn()} />)

      expect(
        screen.getByText('Nhóm Dự Án Lập Trình Mạng Phân Tán Với Tên Cực Kỳ Dài Nhằm Kiểm Thử Độ Co Giãn Layout Mobile')
      ).toHaveClass('truncate')
      expect(screen.getByText('99')).toBeInTheDocument()
    })
  })

  describe('ChatHeader & ChatMessagesArea: Responsive & Text Wrapping', () => {
    it('ChatHeader có role banner và nút quay lại mobile có aria-label', () => {
      const handleBack = vi.fn()
      renderWithProviders(
        <ChatHeader
          title='Nhóm Dự Án Kỹ Thuật Phần Mềm Với Tên Dài'
          isGroup={true}
          onBack={handleBack}
          onOpenInfo={vi.fn()}
        />
      )

      const banner = screen.getByRole('banner')
      expect(banner).toHaveAttribute('aria-label', 'Cuộc trò chuyện: Nhóm Dự Án Kỹ Thuật Phần Mềm Với Tên Dài')

      const backBtn = screen.getByRole('button', { name: 'Quay lại danh sách' })
      expect(backBtn).toBeInTheDocument()

      const settingsBtn = screen.getByRole('button', { name: 'Cài đặt nhóm chat' })
      expect(settingsBtn).toBeInTheDocument()
    })

    it('ChatMessagesArea hỗ trợ [overflow-wrap:anywhere] cho nội dung tin nhắn siêu dài không khoảng trắng', () => {
      useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

      const longUrlMessage: Message = {
        id: 'msg-long-url',
        clientMessageId: 'client-long-url',
        conversationId: mockGroupConversationDetail.id,
        senderId: mockCurrentUser.id,
        content:
          'https://example.com/very/long/url/path/with/extremely/long/token/without/any/spaces/abcdefghijklmnopqrstuvwxyz0123456789_abcdefghijklmnopqrstuvwxyz0123456789',
        createdAt: '2026-11-03T10:00:00Z',
        sender: mockCurrentUser,
        attachments: []
      }

      renderWithProviders(
        <ChatMessagesArea
          conversationId={mockGroupConversationDetail.id}
          title={mockGroupConversationDetail.name || 'Team Backend'}
          isGroup={true}
          optimisticMessages={[longUrlMessage]}
        />
      )

      const messageContent = screen.getByText(longUrlMessage.content!)
      expect(messageContent).toHaveClass('[overflow-wrap:anywhere]')
      expect(messageContent).toHaveClass('break-words')
      expect(messageContent).toHaveClass('min-w-0')
    })

    it('ChatMessagesArea hiển thị nút cuộn xuống cuối khi người dùng cuộn lên trên và ẩn đi khi bấm cuộn xuống', () => {
      useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

      renderWithProviders(
        <ChatMessagesArea
          conversationId={mockGroupConversationDetail.id}
          title={mockGroupConversationDetail.name || 'Team Backend'}
          isGroup={true}
        />
      )

      const logContainer = screen.getByRole('log', { name: 'Tin nhắn' })
      expect(screen.queryByRole('button', { name: 'Cuộn xuống tin nhắn mới nhất' })).not.toBeInTheDocument()

      // Giả lập người dùng cuộn lên trên để đọc tin cũ (cách đáy > 100px)
      Object.defineProperty(logContainer, 'scrollHeight', { configurable: true, value: 1200 })
      Object.defineProperty(logContainer, 'clientHeight', { configurable: true, value: 600 })
      Object.defineProperty(logContainer, 'scrollTop', { configurable: true, writable: true, value: 300 })
      fireEvent.scroll(logContainer)

      // Nút cuộn xuống đáy xuất hiện
      const scrollBottomBtn = screen.getByRole('button', { name: 'Cuộn xuống tin nhắn mới nhất' })
      expect(scrollBottomBtn).toBeInTheDocument()

      // Bấm vào nút cuộn xuống đáy
      fireEvent.click(scrollBottomBtn)

      // Nút biến mất khi cuộn xuống đáy
      expect(screen.queryByRole('button', { name: 'Cuộn xuống tin nhắn mới nhất' })).not.toBeInTheDocument()
    })

    it('ChatMessagesArea cộng dồn số tin nhắn mới kiểu Zalo khi có tin nhắn mới đến trong lúc người dùng đang ở trên', () => {
      useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

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

      expect(screen.getByRole('button', { name: 'Cuộn xuống tin nhắn mới nhất' })).toBeInTheDocument()

      // Tin nhắn mới đến qua optimisticMessages
      const newIncomingMsg: Message = {
        id: 'msg-realtime-zalo-1',
        clientMessageId: 'c-zalo-1',
        conversationId: mockGroupConversationDetail.id,
        senderId: mockPeerUserOnline.id,
        sender: mockPeerUserOnline,
        content: 'Tin nhắn mới tới khi đang đọc tin cũ!',
        createdAt: '2026-11-04T09:20:00Z',
        attachments: []
      }

      rerender(
        <ChatMessagesArea
          conversationId={mockGroupConversationDetail.id}
          title={mockGroupConversationDetail.name || 'Team Backend'}
          isGroup={true}
          optimisticMessages={[newIncomingMsg]}
        />
      )

      // Nút hiển thị dạng pill với số đếm kiểu Zalo: "1 tin nhắn mới"
      expect(screen.getByText('1 tin nhắn mới')).toBeInTheDocument()
      expect(
        screen.getByRole('button', { name: 'Cuộn xuống tin nhắn mới nhất, có 1 tin nhắn mới' })
      ).toBeInTheDocument()

      // Bấm nút cuộn xuống -> biến mất và reset
      fireEvent.click(screen.getByRole('button', { name: 'Cuộn xuống tin nhắn mới nhất, có 1 tin nhắn mới' }))
      expect(screen.queryByText('1 tin nhắn mới')).not.toBeInTheDocument()
    })

    it('ChatMessagesArea tự động cuộn xuống cuối và KHÔNG đếm tin nhắn mới khi chính người dùng gửi tin nhắn trong lúc đang ở trên', () => {
      useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

      const { rerender } = renderWithProviders(
        <ChatMessagesArea
          conversationId={mockGroupConversationDetail.id}
          title={mockGroupConversationDetail.name || 'Team Backend'}
          isGroup={true}
          optimisticMessages={[]}
        />
      )

      const logContainer = screen.getByRole('log', { name: 'Tin nhắn' })

      // Cuộn lên trên để đọc tin cũ
      Object.defineProperty(logContainer, 'scrollHeight', { configurable: true, value: 1200 })
      Object.defineProperty(logContainer, 'clientHeight', { configurable: true, value: 600 })
      Object.defineProperty(logContainer, 'scrollTop', { configurable: true, writable: true, value: 200 })
      fireEvent.scroll(logContainer)

      // Nút cuộn xuống đáy xuất hiện
      expect(screen.getByRole('button', { name: 'Cuộn xuống tin nhắn mới nhất' })).toBeInTheDocument()

      // Người dùng tự gửi tin nhắn mới
      const ownMessage: Message = {
        id: 'msg-own-1',
        clientMessageId: 'c-own-1',
        conversationId: mockGroupConversationDetail.id,
        senderId: mockCurrentUser.id,
        sender: mockCurrentUser,
        content: 'Tin nhắn do chính tôi vừa gửi đi',
        createdAt: '2026-11-04T09:25:00Z',
        attachments: []
      }

      rerender(
        <ChatMessagesArea
          conversationId={mockGroupConversationDetail.id}
          title={mockGroupConversationDetail.name || 'Team Backend'}
          isGroup={true}
          optimisticMessages={[ownMessage]}
        />
      )

      // Không bao giờ hiển thị thông báo "1 tin nhắn mới" cho tin nhắn chính chủ
      expect(screen.queryByText(/tin nhắn mới/)).not.toBeInTheDocument()
      // Tự động cuộn xuống cuối và ẩn nút cuộn
      expect(screen.queryByRole('button', { name: /Cuộn xuống/ })).not.toBeInTheDocument()
    })
  })
})
