import { keepPreviousData, useQuery } from '@tanstack/react-query'
import conversationApi, { type GetConversationsParams } from '~/apis/conversation.api'
import type { ConversationSummary } from '~/types/conversation.type'
import type { Page } from '~/types/pagination.type'

export const conversationKeys = {
  all: ['conversations'] as const,
  list: (params?: GetConversationsParams) => ['conversations', 'list', params] as const
}

const getConversations = async (params?: GetConversationsParams): Promise<Page<ConversationSummary>> => {
  const response = await conversationApi.getConversations(params)
  return response.data
}

export const useConversationsQuery = (params?: GetConversationsParams) => {
  return useQuery<Page<ConversationSummary>, Error>({
    queryKey: conversationKeys.list(params),
    queryFn: () => getConversations(params),
    staleTime: 10_000,
    placeholderData: keepPreviousData
  })
}
