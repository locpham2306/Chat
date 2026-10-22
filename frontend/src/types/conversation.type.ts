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

export type ConversationSummary = Conversation & {
  lastMessage: Message | null
  unreadCount: number
}
