import { v4 as uuidv4 } from 'uuid'
import type { File } from '~/types/file.type'
import type { Message, MessageSender } from '~/types/message.type'
import type { MessageAckPayload, SendMessagePayload } from '~/types/websocket.type'

type SendFrame = (event: 'SEND_MESSAGE', payload: SendMessagePayload) => boolean

interface SendMessageInput {
  conversationId?: string
  recipientId?: string
  content: string
  attachments?: File[]
  currentUser?: MessageSender
  clientMessageId: string
  createdAt: string
}

export interface MessageSendDraft {
  content: string
  attachments: File[]
}

type SendMessageResult = { sent: true; message: Message } | { sent: false }

export interface OutgoingMessage {
  payload: SendMessagePayload
  message: Message
}

export interface MessageDeliveryState {
  status: 'pending' | 'failed'
  error?: string
}

interface QueueDeliveryState extends MessageDeliveryState {
  retryOnReconnect: boolean
}

const NETWORK_SEND_ERROR = 'Mất kết nối. Tin nhắn chưa được gửi.'

export class MessageSendQueue {
  private readonly pending = new Map<string, OutgoingMessage>()
  private readonly failed = new Set<string>()
  private readonly locallySent = new Set<string>()
  private readonly delivery = new Map<string, QueueDeliveryState>()
  private readonly draftRecipientByClientId = new Map<string, string>()

  public enqueue(outgoing: OutgoingMessage): void {
    this.pending.set(outgoing.payload.clientMessageId, outgoing)
    this.locallySent.add(outgoing.payload.clientMessageId)
    this.delivery.set(outgoing.payload.clientMessageId, { status: 'pending', retryOnReconnect: true })
    if (outgoing.payload.recipientId) {
      this.draftRecipientByClientId.set(outgoing.payload.clientMessageId, outgoing.payload.recipientId)
    }
  }

  public attempt(clientMessageId: string, sendFrame: SendFrame): boolean {
    const outgoing = this.pending.get(clientMessageId)
    if (!outgoing) return false

    const sent = sendFrame('SEND_MESSAGE', outgoing.payload)
    if (sent) {
      this.failed.delete(clientMessageId)
      this.delivery.set(clientMessageId, { status: 'pending', retryOnReconnect: true })
    } else {
      this.failed.add(clientMessageId)
      this.delivery.set(clientMessageId, {
        status: 'failed',
        error: NETWORK_SEND_ERROR,
        retryOnReconnect: true
      })
    }
    return sent
  }

  public retry(clientMessageId: string, sendFrame: SendFrame): boolean {
    return this.attempt(clientMessageId, sendFrame)
  }

  public markFailed(clientMessageId: string, error = 'Không gửi được tin nhắn.', retryOnReconnect = false): boolean {
    if (!this.pending.has(clientMessageId)) return false
    this.failed.add(clientMessageId)
    this.delivery.set(clientMessageId, { status: 'failed', error, retryOnReconnect })
    return true
  }

  public retryUnresolved(sendFrame: SendFrame): void {
    for (const [clientMessageId, state] of this.delivery) {
      if (state.retryOnReconnect) this.attempt(clientMessageId, sendFrame)
    }
  }

  public markUnresolvedFailed(error: string): void {
    for (const [clientMessageId, state] of this.delivery) {
      if (!state.retryOnReconnect) continue
      this.failed.add(clientMessageId)
      this.delivery.set(clientMessageId, { status: 'failed', error, retryOnReconnect: false })
    }
  }

  public findFailed(conversationId: string, content: string, attachmentIds: string[]): string | undefined {
    for (const clientMessageId of this.failed) {
      const outgoing = this.pending.get(clientMessageId)
      const outgoingAttachmentIds = outgoing?.payload.attachmentIds ?? []
      const hasSameAttachments =
        outgoingAttachmentIds.length === attachmentIds.length &&
        outgoingAttachmentIds.every((id, index) => id === attachmentIds[index])
      if (
        outgoing?.message.conversationId === conversationId &&
        outgoing.message.content === (content.trim() || null) &&
        hasSameAttachments
      ) {
        return clientMessageId
      }
    }
    return undefined
  }

  public resolve(clientMessageId: string): void {
    this.pending.delete(clientMessageId)
    this.failed.delete(clientMessageId)
    this.delivery.delete(clientMessageId)
  }

  public isLocallySent(clientMessageId: string): boolean {
    return this.locallySent.has(clientMessageId)
  }

  public isLocallySentToRecipient(clientMessageId: string, recipientId: string): boolean {
    return this.locallySent.has(clientMessageId) && this.draftRecipientByClientId.get(clientMessageId) === recipientId
  }

  public failedClientMessageIds(): string[] {
    return [...this.failed]
  }

  public deliveryStates(): Record<string, MessageDeliveryState> {
    return Object.fromEntries(
      [...this.delivery].map(([clientMessageId, { status, error }]) => [
        clientMessageId,
        error ? { status, error } : { status }
      ])
    )
  }
}

export const createClientMessageId = () => uuidv4()

export const createOutgoingMessage = (input: SendMessageInput): OutgoingMessage | null => {
  const content = input.content.trim()
  const attachments = input.attachments ?? []
  const attachmentIds = attachments.map((file) => file.id)
  const conversationId = input.conversationId
  const recipientId = conversationId ? undefined : input.recipientId

  if ((!content && attachmentIds.length === 0) || (!conversationId && !recipientId)) return null

  return {
    payload: {
      ...(conversationId ? { conversationId } : { recipientId }),
      clientMessageId: input.clientMessageId,
      content: content || null,
      ...(attachmentIds.length > 0 ? { attachmentIds } : {})
    },
    message: {
      id: `pending:${input.clientMessageId}`,
      conversationId: conversationId ?? `draft:${recipientId}`,
      ...(input.currentUser
        ? {
            senderId: input.currentUser.id,
            sender: input.currentUser
          }
        : {}),
      clientMessageId: input.clientMessageId,
      content: content || null,
      attachments,
      createdAt: input.createdAt
    }
  }
}

export const sendMessage = (input: SendMessageInput, sendFrame: SendFrame): SendMessageResult => {
  const outgoing = createOutgoingMessage(input)
  if (!outgoing || !sendFrame('SEND_MESSAGE', outgoing.payload)) return { sent: false }

  return {
    sent: true,
    message: outgoing.message
  }
}

export const applyMessageAck = (messages: Message[], payload: MessageAckPayload): Message[] =>
  messages.map((message) =>
    message.clientMessageId === payload.clientMessageId
      ? {
          ...message,
          id: payload.messageId,
          conversationId: payload.conversationId,
          createdAt: payload.createdAt
        }
      : message
  )
