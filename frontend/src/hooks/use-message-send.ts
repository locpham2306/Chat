import { useCallback, useEffect, useRef, useState } from 'react'
import {
  MessageSendQueue,
  applyMessageAck,
  createClientMessageId,
  createOutgoingMessage,
  type MessageDeliveryState,
  type MessageSendDraft
} from '~/lib/message-send'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { Message } from '~/types/message.type'
import { useWebSocketEvent } from '~/hooks/use-websocket'
import { useWebSocketStore } from '~/stores/use-websocket-store'

interface UseMessageSendOptions {
  conversationId?: string
  recipientId?: string | null
}

export const useMessageSend = ({ conversationId, recipientId }: UseMessageSendOptions) => {
  const currentUser = useAuthStore((state) => state.user)
  const webSocketStatus = useWebSocketStore((state) => state.status)
  const [optimisticMessages, setOptimisticMessages] = useState<Message[]>([])
  const [messageDeliveryStates, setMessageDeliveryStates] = useState<Record<string, MessageDeliveryState>>({})
  const queue = useRef(new MessageSendQueue())

  const syncDeliveryStates = useCallback(() => {
    setMessageDeliveryStates(queue.current.deliveryStates())
  }, [])

  const attempt = useCallback(
    (clientMessageId: string) => {
      const sent = queue.current.attempt(clientMessageId, (event, payload) => wsClient.send(event, payload))
      syncDeliveryStates()
      return sent
    },
    [syncDeliveryStates]
  )

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
      if (existingFailedMessageId) {
        attempt(existingFailedMessageId)
        return true
      }

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
      attempt(outgoing.payload.clientMessageId)
      return true
    },
    [attempt, conversationId, currentUser, recipientId]
  )

  useWebSocketEvent('MESSAGE_ACK', (payload) => {
    if (!queue.current.isLocallySent(payload.clientMessageId)) return
    queue.current.resolve(payload.clientMessageId)
    syncDeliveryStates()
    setOptimisticMessages((messages) => applyMessageAck(messages, payload))
  })

  useWebSocketEvent('NEW_MESSAGE', (message) => {
    if (!queue.current.isLocallySent(message.clientMessageId)) return
    queue.current.resolve(message.clientMessageId)
    syncDeliveryStates()
    setOptimisticMessages((messages) => messages.filter((item) => item.clientMessageId !== message.clientMessageId))
  })

  useWebSocketEvent('ERROR', (error) => {
    if (!error.clientMessageId) {
      queue.current.markUnresolvedFailed(error.message)
      syncDeliveryStates()
      return
    }
    if (!queue.current.markFailed(error.clientMessageId, error.message, false)) return
    syncDeliveryStates()
  })

  useEffect(() => {
    if (webSocketStatus !== 'OPEN') return
    queue.current.retryUnresolved((event, payload) => wsClient.send(event, payload))
    syncDeliveryStates()
  }, [syncDeliveryStates, webSocketStatus])

  const hasLocallySentMessageToRecipient = useCallback(
    (clientMessageId: string, targetRecipientId: string) =>
      queue.current.isLocallySentToRecipient(clientMessageId, targetRecipientId),
    []
  )

  const retry = useCallback(
    (clientMessageId: string) => {
      return attempt(clientMessageId)
    },
    [attempt]
  )

  return {
    optimisticMessages,
    messageDeliveryStates,
    send,
    hasLocallySentMessageToRecipient,
    retry
  }
}
