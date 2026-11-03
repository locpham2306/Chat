import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ConversationItem } from '~/components/conversation-list/conversation-item'
import { mockDirectConversationSummary, mockGroupConversationSummary } from './mocks/mock-data'
import { renderWithProviders } from './test-utils'

describe('FE-06 & FE-10: ConversationItem & Danh Sách Hội Thoại', () => {
  it('hiển thị đúng thông tin hội thoại trực tiếp và badge unread count', () => {
    const handleSelect = vi.fn()
    renderWithProviders(
      <ConversationItem conversation={mockDirectConversationSummary} isActive={false} onSelect={handleSelect} />
    )

    // Tên đối phương
    expect(screen.getByText('Nguyễn Văn An')).toBeInTheDocument()

    // Preview tin nhắn cuối
    expect(screen.getByText(/Chào Bình, test API nhé!/)).toBeInTheDocument()

    // Unread count badge (3)
    expect(screen.getByText('3')).toBeInTheDocument()

    // Chấm xanh online cho hội thoại trực tiếp khi peer ONLINE
    const onlineIndicator = screen.getByTitle('Đang trực tuyến')
    expect(onlineIndicator).toBeInTheDocument()
  })

  it('không hiển thị chấm xanh khi đối tác OFFLINE hoặc đối với hội thoại nhóm', () => {
    const handleSelect = vi.fn()
    const offlineConv = {
      ...mockDirectConversationSummary,
      peer: {
        ...mockDirectConversationSummary.peer!,
        status: 'OFFLINE'
      }
    }

    const { rerender } = renderWithProviders(
      <ConversationItem conversation={offlineConv} isActive={false} onSelect={handleSelect} />
    )
    expect(screen.queryByTitle('Đang trực tuyến')).not.toBeInTheDocument()

    // Với nhóm thì không hiển thị chấm online
    rerender(<ConversationItem conversation={mockGroupConversationSummary} isActive={false} onSelect={handleSelect} />)
    expect(screen.queryByTitle('Đang trực tuyến')).not.toBeInTheDocument()
  })

  it('gọi callback onSelect khi người dùng click vào cuộc trò chuyện', () => {
    const handleSelect = vi.fn()
    renderWithProviders(
      <ConversationItem conversation={mockDirectConversationSummary} isActive={false} onSelect={handleSelect} />
    )

    const item = screen.getByRole('button')
    fireEvent.click(item)

    expect(handleSelect).toHaveBeenCalledWith(mockDirectConversationSummary.id)
  })

  it('hỗ trợ điều hướng bằng phím Enter và Space (Accessibility)', () => {
    const handleSelect = vi.fn()
    renderWithProviders(
      <ConversationItem conversation={mockGroupConversationSummary} isActive={false} onSelect={handleSelect} />
    )

    const item = screen.getByRole('button')
    fireEvent.keyDown(item, { key: 'Enter' })
    expect(handleSelect).toHaveBeenCalledWith(mockGroupConversationSummary.id)

    fireEvent.keyDown(item, { key: ' ' })
    expect(handleSelect).toHaveBeenCalledWith(mockGroupConversationSummary.id)
  })
})
