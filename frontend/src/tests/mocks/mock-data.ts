import type { ConversationDetail, ConversationMember, ConversationSummary } from '~/types/conversation.type'
import type { Message } from '~/types/message.type'
import type { User } from '~/types/user.type'

export const mockCurrentUser: User = {
  id: 'user-me-1111-1111-111111111111',
  username: 'giabinh',
  fullName: 'Trần Gia Bình',
  avatarFileId: null,
  status: 'ONLINE',
  lastSeen: null
}

export const mockPeerUserOnline: User = {
  id: 'user-peer-2222-2222-222222222222',
  username: 'vanan',
  fullName: 'Nguyễn Văn An',
  avatarFileId: null,
  status: 'ONLINE',
  lastSeen: null
}

export const mockPeerUserOffline: User = {
  id: 'user-peer-3333-3333-333333333333',
  username: 'hoangnam',
  fullName: 'Lê Hoàng Nam',
  avatarFileId: null,
  status: 'OFFLINE',
  lastSeen: '2026-11-02T10:00:00Z'
}

export const mockDirectConversationSummary: ConversationSummary = {
  id: 'conv-direct-0001',
  type: 'DIRECT',
  createdAt: '2026-11-01T08:00:00Z',
  memberIds: [mockCurrentUser.id, mockPeerUserOnline.id],
  name: null,
  peer: {
    id: mockPeerUserOnline.id,
    username: mockPeerUserOnline.username,
    fullName: mockPeerUserOnline.fullName,
    avatarFileId: null,
    status: 'ONLINE'
  },
  lastMessage: {
    id: 'msg-001',
    conversationId: 'conv-direct-0001',
    clientMessageId: 'client-msg-001',
    content: 'Chào Bình, test API nhé!',
    senderId: mockPeerUserOnline.id,
    sender: mockPeerUserOnline,
    attachments: [],
    createdAt: '2026-11-02T10:51:00Z'
  },
  unreadCount: 3
}

export const mockGroupConversationSummary: ConversationSummary = {
  id: 'conv-group-0002',
  type: 'GROUP',
  createdAt: '2026-10-25T08:00:00Z',
  name: 'Team Backend',
  avatarFileId: null,
  ownerId: mockCurrentUser.id,
  memberCount: 3,
  lastMessage: {
    id: 'msg-002',
    conversationId: 'conv-group-0002',
    clientMessageId: 'client-msg-002',
    content: 'Mai họp dự án lúc 9h nhé',
    senderId: mockCurrentUser.id,
    sender: mockCurrentUser,
    attachments: [],
    createdAt: '2026-11-02T09:00:00Z'
  },
  unreadCount: 0
}

export const mockDirectConversationDetail: ConversationDetail = {
  id: 'conv-direct-0001',
  type: 'DIRECT',
  name: null,
  avatarFileId: null,
  avatarUrl: null,
  ownerId: null,
  memberCount: 2,
  createdAt: '2026-11-01T08:00:00Z',
  peer: {
    id: mockPeerUserOnline.id,
    username: mockPeerUserOnline.username,
    fullName: mockPeerUserOnline.fullName,
    avatarFileId: null,
    status: 'ONLINE'
  }
}

export const mockGroupConversationDetail: ConversationDetail = {
  id: 'conv-group-0002',
  type: 'GROUP',
  name: 'Team Backend',
  avatarFileId: null,
  avatarUrl: null,
  ownerId: mockCurrentUser.id,
  memberCount: 3,
  createdAt: '2026-10-25T08:00:00Z'
}

export const mockGroupMembers: ConversationMember[] = [
  {
    user: mockCurrentUser,
    role: 'OWNER',
    joinedAt: '2026-10-25T08:00:00Z'
  },
  {
    user: mockPeerUserOnline,
    role: 'MEMBER',
    joinedAt: '2026-10-25T08:05:00Z'
  },
  {
    user: mockPeerUserOffline,
    role: 'MEMBER',
    joinedAt: '2026-10-26T09:00:00Z'
  }
]

export const mockSampleMessages: Message[] = [
  {
    id: 'msg-101',
    conversationId: 'conv-direct-0001',
    clientMessageId: 'c-msg-101',
    content: 'Tin nhắn thứ nhất từ An',
    senderId: mockPeerUserOnline.id,
    sender: mockPeerUserOnline,
    attachments: [],
    createdAt: '2026-11-02T10:00:00Z'
  },
  {
    id: 'msg-102',
    conversationId: 'conv-direct-0001',
    clientMessageId: 'c-msg-102',
    content: 'Tin nhắn thứ hai gom nhóm (cùng người gửi trong 5p)',
    senderId: mockPeerUserOnline.id,
    sender: mockPeerUserOnline,
    attachments: [],
    createdAt: '2026-11-02T10:02:00Z'
  },
  {
    id: 'msg-103',
    conversationId: 'conv-direct-0001',
    clientMessageId: 'c-msg-103',
    content: 'Tin nhắn từ Bình (tách nhóm vì đổi người gửi)',
    senderId: mockCurrentUser.id,
    sender: mockCurrentUser,
    attachments: [],
    createdAt: '2026-11-02T10:04:00Z'
  },
  {
    id: 'msg-104',
    conversationId: 'conv-direct-0001',
    clientMessageId: 'c-msg-104',
    content: 'Ảnh minh họa giao diện',
    senderId: mockPeerUserOnline.id,
    sender: mockPeerUserOnline,
    attachments: [
      {
        id: 'file-img-01',
        name: 'screenshot.png',
        contentType: 'image/png',
        size: 102400,
        downloadUrl: '/api/files/file-img-01'
      }
    ],
    createdAt: '2026-11-02T10:06:00Z'
  }
]
