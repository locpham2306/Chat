import { describe, expect, it, vi } from 'vitest'
import { applyMessageAck, createOutgoingMessage, MessageSendQueue } from './message-send'

const outgoing = () => {
  const value = createOutgoingMessage({
    recipientId: 'u2',
    content: '  Xin chào  ',
    clientMessageId: 'client-1',
    createdAt: '2026-11-03T10:00:00Z'
  })
  if (!value) throw new Error('Expected outgoing message')
  return value
}

describe('message send and retry', () => {
  it('keeps the exact payload and clientMessageId when retrying a network failure', () => {
    const queue = new MessageSendQueue()
    const message = outgoing()
    const firstSend = vi.fn().mockReturnValue(false)
    const retrySend = vi.fn().mockReturnValue(true)
    queue.enqueue(message)

    expect(queue.attempt('client-1', firstSend)).toBe(false)
    expect(queue.deliveryStates()).toEqual({
      'client-1': { status: 'failed', error: 'Mất kết nối. Tin nhắn chưa được gửi.' }
    })

    expect(queue.retry('client-1', retrySend)).toBe(true)
    expect(firstSend.mock.calls[0][1]).toBe(message.payload)
    expect(retrySend.mock.calls[0][1]).toBe(message.payload)
    expect(retrySend.mock.calls[0][1]).toEqual({
      recipientId: 'u2',
      clientMessageId: 'client-1',
      content: 'Xin chào'
    })
  })

  it('does not automatically retry a server-rejected message on reconnect', () => {
    const queue = new MessageSendQueue()
    queue.enqueue(outgoing())
    queue.markFailed('client-1', 'Bạn không có quyền gửi tin nhắn.', false)
    const send = vi.fn()

    queue.retryUnresolved(send)

    expect(send).not.toHaveBeenCalled()
    expect(queue.deliveryStates()['client-1']).toEqual({
      status: 'failed',
      error: 'Bạn không có quyền gửi tin nhắn.'
    })
  })

  it('replaces optimistic identifiers with MESSAGE_ACK data', () => {
    const message = outgoing().message

    expect(
      applyMessageAck([message], {
        clientMessageId: 'client-1',
        messageId: 'm1',
        conversationId: 'c1',
        createdAt: '2026-11-03T10:00:01Z'
      })[0]
    ).toMatchObject({ id: 'm1', conversationId: 'c1', clientMessageId: 'client-1' })
  })
})
