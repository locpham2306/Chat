import type { Message } from './message.type'

export type ConversationType = 'DIRECT' | 'GROUP'

interface BaseConversation {
  id: string
  type: ConversationType
  createdAt: string
}

export interface DirectConversation extends BaseConversation {
  type: 'DIRECT'
  memberIds?: string[]
}

export interface GroupConversation extends BaseConversation {
  type: 'GROUP'
  name: string
  avatarFileId: string | null
  ownerId?: string | null
  memberCount?: number
}

export type Conversation = DirectConversation | GroupConversation

export interface ConversationSummary {
  id: string
  type: ConversationType
  name?: string | null
  avatarFileId?: string | null
  ownerId?: string | null
  memberCount?: number
  createdAt: string
  lastMessage?: Message | null
  unreadCount: number
}
