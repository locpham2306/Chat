import type { File } from './file.type'

export interface Message {
  id: string
  conversationId: string
  senderId: string
  clientMessageId: string
  content: string | null
  attachments: File[]
  createdAt: string
}
