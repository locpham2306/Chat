import type { File } from './file.type'

export interface MessageSender {
  id: string
  username: string
  fullName: string
  avatarFileId: string | null
}

export interface Message {
  id: string
  conversationId: string
  senderId?: string
  sender?: MessageSender | null
  clientMessageId: string
  content: string | null
  attachments: File[]
  createdAt: string
}
