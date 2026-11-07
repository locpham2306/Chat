import { describe, expect, it } from 'vitest'
import { mergeChronologicalMessages } from './use-message-history'
import type { Message, MessageHistoryPage } from '~/types/message.type'

const message = (id: string, createdAt: string, clientMessageId = `client-${id}`): Message => ({
  id,
  conversationId: 'c1',
  senderId: 'u1',
  clientMessageId,
  content: id,
  attachments: [],
  createdAt
})

const page = (data: Message[]): MessageHistoryPage => ({ data, hasMore: false, nextCursor: null })

describe('message history merge', () => {
  it('sorts oldest to newest and removes duplicate message and client IDs', () => {
    const duplicateFromOlderPage = message('m-old-copy', '2026-11-03T09:00:00Z', 'client-1')
    const result = mergeChronologicalMessages([
      page([message('m2', '2026-11-03T10:00:00Z'), message('m1', '2026-11-03T09:00:00Z', 'client-1')]),
      page([duplicateFromOlderPage, message('m0', '2026-11-03T08:00:00Z')])
    ])

    expect(result.map((item) => item.id)).toEqual(['m0', 'm1', 'm2'])
  })
})
