import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { CreateGroupDialog } from '~/components/group-create/create-group-dialog'
import { GroupSettingsDialog } from '~/components/group-settings/group-settings-dialog'
import { useAuthStore } from '~/stores/use-auth-store'
import {
  mockCurrentUser,
  mockGroupConversationDetail,
  mockGroupMembers,
  mockPeerUserOffline,
  mockPeerUserOnline
} from './mocks/mock-data'
import { renderWithProviders } from './test-utils'

// Mock use-user-search
vi.mock('~/hooks/use-user-search', async (importOriginal) => {
  const actual = await importOriginal<typeof import('~/hooks/use-user-search')>()
  return {
    ...actual,
    useUserSearchQuery: () => ({
      data: {
        data: [mockPeerUserOnline, mockPeerUserOffline],
        totalPages: 1,
        totalElements: 2
      },
      isLoading: false,
      isError: false
    })
  }
})

// Mock use-group-members
vi.mock('~/hooks/use-group-members', async (importOriginal) => {
  const actual = await importOriginal<typeof import('~/hooks/use-group-members')>()
  return {
    ...actual,
    useGroupMembersQuery: () => ({
      data: {
        data: mockGroupMembers,
        totalPages: 1,
        totalElements: 3
      },
      isLoading: false,
      isError: false
    }),
    useRemoveGroupMemberMutation: () => ({
      mutate: vi.fn(),
      isPending: false
    }),
    useAddGroupMembersMutation: () => ({
      mutate: vi.fn(),
      isPending: false
    })
  }
})

// Mock use-conversation
vi.mock('~/hooks/use-conversation', () => ({
  useCreateGroupMutation: () => ({
    mutate: vi.fn(),
    isPending: false
  }),
  useUpdateConversationMutation: () => ({
    mutate: vi.fn(),
    isPending: false
  }),
  useConversationDetailQuery: () => ({
    data: mockGroupConversationDetail,
    isLoading: false
  }),
  useMarkConversationReadMutation: () => ({
    mutate: vi.fn(),
    isPending: false
  })
}))

describe('FE-14, FE-16 & FE-18: Quản Lý Nhóm & Cài Đặt Nhóm', () => {
  describe('CreateGroupDialog (FE-14)', () => {
    it('hiển thị form tạo nhóm và danh sách gợi ý thành viên khi mở dialog', () => {
      const handleOpenChange = vi.fn()

      renderWithProviders(<CreateGroupDialog open={true} onOpenChange={handleOpenChange} />)

      expect(screen.getByText('Tạo nhóm chat mới')).toBeInTheDocument()
      expect(screen.getByPlaceholderText(/Nhóm học tập LTM/)).toBeInTheDocument()

      // Nút tạo nhóm ban đầu bị disabled vì chưa nhập tên và chọn thành viên
      const submitBtn = screen.getByRole('button', { name: /Tạo nhóm/ })
      expect(submitBtn).toBeDisabled()
    })
  })

  describe('GroupSettingsDialog: Thông tin & Phân quyền thành viên (FE-16, FE-18)', () => {
    it('hiển thị thông tin nhóm và danh sách thành viên với badge Chủ nhóm cho OWNER', () => {
      useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

      renderWithProviders(
        <GroupSettingsDialog open={true} onOpenChange={vi.fn()} conversation={mockGroupConversationDetail} />
      )

      expect(screen.getByText('Team Backend')).toBeInTheDocument()
      expect(screen.getByText('Cài đặt chung')).toBeInTheDocument()

      // Chuyển sang tab Thành viên
      const membersTab = screen.getByRole('button', { name: /Thành viên/ })
      fireEvent.click(membersTab)

      // Hiển thị danh sách thành viên
      expect(screen.getByText('Nguyễn Văn An')).toBeInTheDocument()
      expect(screen.getByText('Lê Hoàng Nam')).toBeInTheDocument()

      // Badge chủ nhóm
      expect(screen.getByText('Chủ nhóm')).toBeInTheDocument()
    })

    it('hiển thị các nút quản trị (thêm/xóa thành viên) khi người dùng là OWNER', () => {
      useAuthStore.setState({ user: mockCurrentUser, isAuthenticated: true })

      renderWithProviders(
        <GroupSettingsDialog open={true} onOpenChange={vi.fn()} conversation={mockGroupConversationDetail} />
      )

      // Chuyển sang tab Thành viên
      const membersTab = screen.getByRole('button', { name: /Thành viên/ })
      fireEvent.click(membersTab)

      // OWNER có nút thêm thành viên
      expect(screen.getByRole('button', { name: /Thêm thành viên/ })).toBeInTheDocument()

      // OWNER có nút xóa thành viên cho các thành viên khác
      const removeButton = screen.getByLabelText(/Xóa Nguyễn Văn An khỏi nhóm/)
      expect(removeButton).toBeInTheDocument()
    })

    it('chỉ hiển thị nút Rời nhóm và không cho phép chỉnh sửa khi người dùng là MEMBER', () => {
      // Giả lập người dùng hiện tại là Nguyễn Văn An (MEMBER)
      useAuthStore.setState({ user: mockPeerUserOnline, isAuthenticated: true })

      renderWithProviders(
        <GroupSettingsDialog open={true} onOpenChange={vi.fn()} conversation={mockGroupConversationDetail} />
      )

      // Chuyển sang tab Thành viên
      const membersTab = screen.getByRole('button', { name: /Thành viên/ })
      fireEvent.click(membersTab)

      // MEMBER không có nút thêm thành viên
      expect(screen.queryByRole('button', { name: /Thêm thành viên/ })).not.toBeInTheDocument()

      // MEMBER không có nút xóa thành viên khác
      expect(screen.queryByLabelText(/Xóa Lê Hoàng Nam khỏi nhóm/)).not.toBeInTheDocument()

      // MEMBER có nút Rời nhóm
      expect(screen.getByRole('button', { name: /Rời nhóm/ })).toBeInTheDocument()
    })
  })
})
