import type { Message } from './message.type'

export type ConversationType = 'DIRECT' | 'GROUP'

interface BaseConversation {
  id: string
  type: ConversationType
  createdAt: string
}

export interface DirectConversation extends BaseConversation {
  type: 'DIRECT'
  memberIds: string[]
}

export interface GroupConversation extends BaseConversation {
  type: 'GROUP'
  name: string
  avatarFileId: string | null
  ownerId: string
  memberCount: number
}

export type Conversation = DirectConversation | GroupConversation

export interface ConversationPeer {
  id: string
  username: string
  fullName: string
  avatarFileId: string | null
  avatarUrl?: string | null
  status?: string
}

export interface ConversationDetail {
  id: string
  type: ConversationType
  name: string | null
  avatarFileId: string | null
  avatarUrl?: string | null
  ownerId: string | null
  memberCount: number
  createdAt: string
  peer?: ConversationPeer | null
}

export interface DirectConversationLookupResult {
  conversation: DirectConversation | null
  peer?: ConversationPeer | null
}

export type ConversationSummary = Conversation & {
  name?: string | null
  avatarFileId?: string | null
  avatarUrl?: string | null
  peer?: ConversationPeer | null
  lastMessage: Message | null
  unreadCount: number
}

export interface CreateGroupBody {
  name: string
  memberIds: string[]
  avatarFileId?: string | null
}

export interface UpdateConversationBody {
  name: string
  avatarFileId: string | null
}
