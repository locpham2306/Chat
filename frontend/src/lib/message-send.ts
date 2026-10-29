import type { Message, MessageSender } from '~/types/message.type'
import type { MessageAckPayload, SendMessagePayload } from '~/types/websocket.type'

type SendFrame = (event: 'SEND_MESSAGE', payload: SendMessagePayload) => boolean

interface SendMessageInput {
  conversationId?: string
  recipientId?: string
  content: string
  currentUser?: MessageSender
  clientMessageId: string
  createdAt: string
}

type SendMessageResult = { sent: true; message: Message } | { sent: false }

export interface OutgoingMessage {
  payload: SendMessagePayload
  message: Message
}

export class MessageSendQueue {
  private readonly pending = new Map<string, OutgoingMessage>()
  private readonly failed = new Set<string>()
  private readonly locallySent = new Set<string>()

  public enqueue(outgoing: OutgoingMessage): void {
    this.pending.set(outgoing.payload.clientMessageId, outgoing)
    this.locallySent.add(outgoing.payload.clientMessageId)
  }

  public attempt(clientMessageId: string, sendFrame: SendFrame): boolean {
    const outgoing = this.pending.get(clientMessageId)
    if (!outgoing) return false

    const sent = sendFrame('SEND_MESSAGE', outgoing.payload)
    if (sent) {
      this.failed.delete(clientMessageId)
    } else {
      this.failed.add(clientMessageId)
    }
    return sent
  }

  public retry(clientMessageId: string, sendFrame: SendFrame): boolean {
    return this.attempt(clientMessageId, sendFrame)
  }

  public markFailed(clientMessageId: string): boolean {
    if (!this.pending.has(clientMessageId)) return false
    this.failed.add(clientMessageId)
    return true
  }

  public findFailed(conversationId: string, content: string): string | undefined {
    for (const clientMessageId of this.failed) {
      const outgoing = this.pending.get(clientMessageId)
      if (outgoing?.message.conversationId === conversationId && outgoing.message.content === content) {
        return clientMessageId
      }
    }
    return undefined
  }

  public resolve(clientMessageId: string): void {
    this.pending.delete(clientMessageId)
    this.failed.delete(clientMessageId)
  }

  public isLocallySent(clientMessageId: string): boolean {
    return this.locallySent.has(clientMessageId)
  }

  public failedClientMessageIds(): string[] {
    return [...this.failed]
  }
}

export const createClientMessageId = () => crypto.randomUUID()

export const createOutgoingMessage = (input: SendMessageInput): OutgoingMessage | null => {
  const content = input.content.trim()
  const conversationId = input.conversationId
  const recipientId = conversationId ? undefined : input.recipientId

  if (!content || (!conversationId && !recipientId)) return null

  return {
    payload: {
      ...(conversationId ? { conversationId } : { recipientId }),
      clientMessageId: input.clientMessageId,
      content
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
      content,
      attachments: [],
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
