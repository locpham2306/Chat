import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ServerEvent, ServerEventMap } from '~/types/websocket.type'

const { listeners, sendFrame } = vi.hoisted(() => ({
  listeners: {} as Partial<{ [E in ServerEvent]: (payload: ServerEventMap[E]) => void }>,
  sendFrame: vi.fn()
}))

vi.mock('~/lib/websocket-client', () => ({
  wsClient: {
    send: sendFrame,
    on: vi.fn((event: ServerEvent, handler: (payload: never) => void) => {
      listeners[event] = handler as never
      return () => delete listeners[event]
    })
  }
}))

import { useMessageSend } from '~/hooks/use-message-send'
import { useWebSocketStore } from '~/stores/use-websocket-store'

describe('useMessageSend', () => {
  beforeEach(() => {
    sendFrame.mockReset()
    Object.keys(listeners).forEach((event) => delete listeners[event as ServerEvent])
    useWebSocketStore.getState().reset()
  })

  it('keeps a failed draft and manually retries the identical payload', () => {
    sendFrame.mockReturnValueOnce(false).mockReturnValueOnce(true)
    const { result } = renderHook(() => useMessageSend({ conversationId: 'conversation-1' }))

    act(() => {
      expect(result.current.send({ content: 'Xin chào', attachments: [] })).toBe(true)
    })
    const firstPayload = sendFrame.mock.calls[0][1]
    expect(result.current.messageDeliveryStates[firstPayload.clientMessageId]).toMatchObject({ status: 'failed' })

    act(() => {
      expect(result.current.retry(firstPayload.clientMessageId)).toBe(true)
    })
    expect(sendFrame.mock.calls[1][1]).toBe(firstPayload)
    expect(result.current.messageDeliveryStates[firstPayload.clientMessageId]).toEqual({ status: 'pending' })
  })

  it('applies WS-07 errors and clears delivery state after retry plus ACK', () => {
    sendFrame.mockReturnValue(true)
    const { result } = renderHook(() => useMessageSend({ conversationId: 'conversation-1' }))

    act(() => {
      result.current.send({ content: 'Xin chào', attachments: [] })
    })
    const clientMessageId = sendFrame.mock.calls[0][1].clientMessageId as string

    act(() => {
      listeners.ERROR?.({ code: 'FORBIDDEN', message: 'Bạn không có quyền gửi tin nhắn.', clientMessageId })
    })
    expect(result.current.messageDeliveryStates[clientMessageId]).toEqual({
      status: 'failed',
      error: 'Bạn không có quyền gửi tin nhắn.'
    })

    act(() => {
      result.current.retry(clientMessageId)
      listeners.MESSAGE_ACK?.({
        clientMessageId,
        conversationId: 'conversation-1',
        messageId: 'message-1',
        createdAt: '2026-11-03T10:00:01Z'
      })
    })
    expect(result.current.messageDeliveryStates[clientMessageId]).toBeUndefined()
    expect(result.current.optimisticMessages[0]).toMatchObject({ id: 'message-1', clientMessageId })
  })

  it('automatically retries the original payload when WebSocket reopens', () => {
    sendFrame.mockReturnValueOnce(false).mockReturnValueOnce(true)
    const { result } = renderHook(() => useMessageSend({ recipientId: 'u2' }))

    act(() => {
      result.current.send({ content: 'Tin đầu tiên', attachments: [] })
    })
    const firstPayload = sendFrame.mock.calls[0][1]

    act(() => useWebSocketStore.getState().setStatus('OPEN'))

    expect(sendFrame).toHaveBeenCalledTimes(2)
    expect(sendFrame.mock.calls[1][1]).toBe(firstPayload)
  })
})
