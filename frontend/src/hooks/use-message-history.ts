import { useInfiniteQuery } from '@tanstack/react-query'
import messageApi from '~/apis/message.api'
import { messageKeys } from '~/hooks/use-realtime-message-sync'
import type { Message, MessageHistoryPage } from '~/types/message.type'

export const getMessageHistory = async (
  conversationId: string,
  before?: string,
  signal?: AbortSignal
): Promise<MessageHistoryPage> => {
  const response = await messageApi.getHistory({ conversationId, before, signal })
  return response.data.content
}

const instantSortKey = (createdAt: string) => {
  const match = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?Z$/.exec(createdAt)
  return match ? `${match[1]}.${(match[2] ?? '').padEnd(9, '0')}` : createdAt
}

export const mergeChronologicalMessages = (pages: MessageHistoryPage[]): Message[] => {
  const ids = new Set<string>()
  const clientIds = new Set<string>()
  const messages: Message[] = []

  // Trang đầu mới nhất; giữ phiên bản từ cache realtime nếu một tin xuất hiện ở hai trang.
  for (const page of pages) {
    for (const message of page.data) {
      const senderId = message.senderId ?? message.sender?.id
      const clientKey = senderId && message.clientMessageId ? `${senderId}:${message.clientMessageId}` : undefined
      if (ids.has(message.id) || (clientKey && clientIds.has(clientKey))) continue
      ids.add(message.id)
      if (clientKey) clientIds.add(clientKey)
      messages.push(message)
    }
  }

  return messages.sort((a, b) => {
    const aTime = instantSortKey(a.createdAt)
    const bTime = instantSortKey(b.createdAt)
    return aTime < bTime ? -1 : aTime > bTime ? 1 : a.id.localeCompare(b.id)
  })
}

export const useMessageHistoryQuery = (conversationId: string, enabled = true) =>
  useInfiniteQuery({
    queryKey: messageKeys.list(conversationId),
    queryFn: ({ pageParam, signal }) => getMessageHistory(conversationId, pageParam, signal),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined),
    enabled: enabled && Boolean(conversationId),
    staleTime: 10_000,
    retry: false
  })
