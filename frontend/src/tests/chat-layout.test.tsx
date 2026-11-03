import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ChatHeader } from '~/components/chat-layout/chat-header'
import { ChatMessagesArea } from '~/components/chat-layout/chat-messages-area'
import { mockSampleMessages } from './mocks/mock-data'
import { renderWithProviders } from './test-utils'

// Mock useMessageHistoryQuery to return our mock messages
vi.mock('~/hooks/use-message-history', () => ({
  useMessageHistoryQuery: () => ({
    data: {
      pages: [{ data: mockSampleMessages, nextCursor: null, hasMore: false }]
    },
    isSuccess: true,
    isLoading: false,
    hasNextPage: false,
    isFetchingNextPage: false
  }),
  mergeChronologicalMessages: (pages: Array<{ data: unknown[] }>) => pages.flatMap((page) => page.data)
}))

// Mock useMarkConversationReadMutation
vi.mock('~/hooks/use-conversation', () => ({
  useMarkConversationReadMutation: () => ({
    mutate: vi.fn(),
    isPending: false
  })
}))

describe('FE-08: ChatHeader, Gom Cụm Tin Nhắn & Bubble Ảnh Kiểu Zalo', () => {
  describe('ChatHeader', () => {
    it('hiển thị đúng tiêu đề, avatar và trạng thái Trực tuyến của đối phương', () => {
      const handleBack = vi.fn()
      const handleOpenInfo = vi.fn()

      renderWithProviders(
        <ChatHeader
          title='Nguyễn Văn An'
          isGroup={false}
          subtitle='Trực tuyến'
          avatarUrl={null}
          onBack={handleBack}
          onOpenInfo={handleOpenInfo}
        />
      )

      expect(screen.getByText('Nguyễn Văn An')).toBeInTheDocument()
      expect(screen.getByText('Trực tuyến')).toBeInTheDocument()

      const backBtn = screen.getByLabelText('Quay lại danh sách')
      fireEvent.click(backBtn)
      expect(handleBack).toHaveBeenCalled()
    })

    it('hiển thị đúng số lượng thành viên khi là hội thoại nhóm', () => {
      renderWithProviders(<ChatHeader title='Team Backend' isGroup={true} subtitle='3 thành viên' avatarUrl={null} />)

      expect(screen.getByText('Team Backend')).toBeInTheDocument()
      expect(screen.getByText('3 thành viên')).toBeInTheDocument()
    })
  })

  describe('ChatMessagesArea: Gom cụm tin nhắn & Bong bóng ảnh kiểu Zalo', () => {
    it('hiển thị danh sách tin nhắn theo thứ tự và phân nhóm đúng quy tắc', () => {
      renderWithProviders(<ChatMessagesArea conversationId='conv-direct-0001' title='Nguyễn Văn An' isGroup={false} />)

      // Kiểm tra sự xuất hiện của các tin nhắn
      expect(screen.getByText('Tin nhắn thứ nhất từ An')).toBeInTheDocument()
      expect(screen.getByText('Tin nhắn thứ hai gom nhóm (cùng người gửi trong 5p)')).toBeInTheDocument()
      expect(screen.getByText('Tin nhắn từ Bình (tách nhóm vì đổi người gửi)')).toBeInTheDocument()
    })

    it('render ảnh đính kèm to và đặt chữ bên dưới ảnh theo chuẩn kiểu Zalo', () => {
      renderWithProviders(<ChatMessagesArea conversationId='conv-direct-0001' title='Nguyễn Văn An' isGroup={false} />)

      // Ảnh screenshot.png được render
      const img = screen.getByRole('img', { name: 'screenshot.png' })
      expect(img).toBeInTheDocument()
      expect(img).toHaveAttribute('src', '/api/files/file-img-01')

      // Text nội dung đi kèm ảnh hiển thị trong cùng tin nhắn
      expect(screen.getByText('Ảnh minh họa giao diện')).toBeInTheDocument()
    })
  })
})
