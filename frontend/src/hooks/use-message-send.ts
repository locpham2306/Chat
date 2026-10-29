import { useCallback, useRef, useState } from 'react'
import {
  MessageSendQueue,
  applyMessageAck,
  createClientMessageId,
  createOutgoingMessage,
  type MessageSendDraft
} from '~/lib/message-send'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { Message } from '~/types/message.type'
import { useWebSocketEvent } from '~/hooks/use-websocket'

interface UseMessageSendOptions {
  conversationId?: string
  recipientId?: string | null
}

export const useMessageSend = ({ conversationId, recipientId }: UseMessageSendOptions) => {
  const currentUser = useAuthStore((state) => state.user)
  const [optimisticMessages, setOptimisticMessages] = useState<Message[]>([])
  const [failedClientMessageIds, setFailedClientMessageIds] = useState<string[]>([])
  const queue = useRef(new MessageSendQueue())

  const attempt = useCallback((clientMessageId: string) => {
    const sent = queue.current.attempt(clientMessageId, (event, payload) => wsClient.send(event, payload))
    setFailedClientMessageIds(queue.current.failedClientMessageIds())
    return sent
  }, [])

  const send = useCallback(
    ({ content, attachments }: MessageSendDraft): boolean => {
      const targetConversationId = conversationId ?? (recipientId ? `draft:${recipientId}` : undefined)
      const existingFailedMessageId = targetConversationId
        ? queue.current.findFailed(
            targetConversationId,
            content,
            attachments.map((attachment) => attachment.id)
          )
        : undefined
      if (existingFailedMessageId) return attempt(existingFailedMessageId)

      const clientMessageId = createClientMessageId()
      const outgoing = createOutgoingMessage({
        conversationId,
        recipientId: recipientId ?? undefined,
        content,
        attachments,
        currentUser: currentUser ?? undefined,
        clientMessageId,
        createdAt: new Date().toISOString()
      })

      if (!outgoing) return false

      queue.current.enqueue(outgoing)
      setOptimisticMessages((messages) => [...messages, outgoing.message])
      return attempt(outgoing.payload.clientMessageId)
    },
    [attempt, conversationId, currentUser, recipientId]
  )

  useWebSocketEvent('MESSAGE_ACK', (payload) => {
    if (!queue.current.isLocallySent(payload.clientMessageId)) return
    queue.current.resolve(payload.clientMessageId)
    setFailedClientMessageIds(queue.current.failedClientMessageIds())
    setOptimisticMessages((messages) => applyMessageAck(messages, payload))
  })

  useWebSocketEvent('NEW_MESSAGE', (message) => {
    if (!queue.current.isLocallySent(message.clientMessageId)) return
    queue.current.resolve(message.clientMessageId)
    setFailedClientMessageIds(queue.current.failedClientMessageIds())
    setOptimisticMessages((messages) => messages.filter((item) => item.clientMessageId !== message.clientMessageId))
  })

  useWebSocketEvent('ERROR', (error) => {
    if (!error.clientMessageId || !queue.current.markFailed(error.clientMessageId)) return
    setFailedClientMessageIds(queue.current.failedClientMessageIds())
  })

  const hasLocallySentMessage = useCallback(
    (clientMessageId: string) => queue.current.isLocallySent(clientMessageId),
    []
  )

  const retry = useCallback((clientMessageId: string) => attempt(clientMessageId), [attempt])

  return { optimisticMessages, failedClientMessageIds, send, hasLocallySentMessage, retry }
}
