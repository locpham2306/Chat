import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import conversationApi, { type GetConversationsParams } from '~/apis/conversation.api'
import type { ApiPage } from '~/types/api-response.type'
import type { ConversationSummary } from '~/types/conversation.type'

export const conversationKeys = {
  all: ['conversations'] as const,
  list: (params?: GetConversationsParams) => ['conversations', 'list', params] as const
}

export type DirectConversationLookupResult =
  { kind: 'existing'; conversationId: string } | { kind: 'new'; recipientId: string }

export const lookupDirectConversation = async (recipientId: string): Promise<DirectConversationLookupResult> => {
  const response = await conversationApi.findDirectConversation(recipientId)

  if (response.status === 204 || !response.data) {
    return { kind: 'new', recipientId }
  }

  return { kind: 'existing', conversationId: response.data.content.id }
}

export const getConversations = async (params?: GetConversationsParams): Promise<ApiPage<ConversationSummary[]>> => {
  const response = await conversationApi.getConversations(params)
  return response.data.content
}

export const useConversationsQuery = (params?: GetConversationsParams) => {
  return useQuery<ApiPage<ConversationSummary[]>, Error>({
    queryKey: conversationKeys.list(params),
    queryFn: () => getConversations(params),
    staleTime: 10_000,
    placeholderData: keepPreviousData
  })
}

export const useDirectConversationMutation = () =>
  useMutation({
    mutationFn: lookupDirectConversation
  })
