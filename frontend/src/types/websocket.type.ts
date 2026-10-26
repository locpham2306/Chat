import type { Message } from './message.type'
import type { UserStatus } from './user.type'

export type WebSocketStatus = 'CONNECTING' | 'OPEN' | 'CLOSING' | 'CLOSED'

// Client -> Server Payloads
export interface SendMessagePayload {
  recipientId?: string
  conversationId?: string
  clientMessageId: string
  content?: string | null
  attachmentIds?: string[]
}

export interface ClientTypingPayload {
  conversationId: string
  isTyping: boolean
}

// Server -> Client Payloads
export interface MessageAckPayload {
  clientMessageId: string
  conversationId: string
  messageId: string
  createdAt: string
}

export type NewMessagePayload = Message

export interface ServerTypingPayload {
  conversationId: string
  userId: string
  isTyping: boolean
}

export interface ReadReceiptPayload {
  conversationId: string
  userId: string
  lastReadMessageId: string
}

export interface PresenceChangedPayload {
  userId: string
  status: UserStatus
  lastSeen: string
}

export type WebSocketErrorCode =
  'VALIDATION_ERROR' | 'UNAUTHENTICATED' | 'FORBIDDEN' | 'NOT_FOUND' | 'CONFLICT' | 'INTERNAL_ERROR'

export interface WebSocketErrorPayload {
  clientMessageId?: string | null
  code: WebSocketErrorCode
  message: string
}

// Event mappings
export interface ClientEventMap {
  SEND_MESSAGE: SendMessagePayload
  TYPING: ClientTypingPayload
}

export interface ServerEventMap {
  MESSAGE_ACK: MessageAckPayload
  NEW_MESSAGE: NewMessagePayload
  TYPING: ServerTypingPayload
  READ_RECEIPT: ReadReceiptPayload
  PRESENCE_CHANGED: PresenceChangedPayload
  ERROR: WebSocketErrorPayload
}

export type ClientEvent = keyof ClientEventMap
export type ServerEvent = keyof ServerEventMap

export interface WebSocketFrame<E extends string = string, D = unknown> {
  event: E
  data: D
}

export type WebSocketListener<T> = (data: T) => void
