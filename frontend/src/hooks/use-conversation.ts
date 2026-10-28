import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import conversationApi, { type GetConversationsParams } from '~/apis/conversation.api'
import type { ApiPage } from '~/types/api-response.type'
import type {
  ConversationDetail,
  ConversationPeer,
  ConversationSummary,
  CreateGroupBody,
  GroupConversation
} from '~/types/conversation.type'

export const conversationKeys = {
  all: ['conversations'] as const,
  list: (params?: GetConversationsParams) => ['conversations', 'list', params] as const,
  detail: (id?: string) => ['conversations', 'detail', id] as const,
  directLookup: (recipientId?: string) => ['conversations', 'direct', recipientId] as const
}

export type DirectConversationLookupResult =
  | { kind: 'existing'; conversationId: string; peer?: ConversationPeer | null }
  | { kind: 'new'; recipientId: string; peer?: ConversationPeer | null }

export const lookupDirectConversation = async (recipientId: string): Promise<DirectConversationLookupResult> => {
  const response = await conversationApi.findDirectConversation(recipientId)

  if (response.status === 204 || !response.data?.content) {
    return { kind: 'new', recipientId }
  }

  const content = response.data.content

  if ('conversation' in content) {
    if (content.conversation?.id) {
      return {
        kind: 'existing',
        conversationId: content.conversation.id,
        peer: content.peer
      }
    }
    return {
      kind: 'new',
      recipientId,
      peer: content.peer
    }
  }

  if ('id' in content && content.id) {
    return { kind: 'existing', conversationId: content.id }
  }

  return { kind: 'new', recipientId }
}

export const getConversationDetail = async (id: string): Promise<ConversationDetail> => {
  const response = await conversationApi.getConversationDetail(id)
  return response.data.content
}

export const useConversationDetailQuery = (conversationId?: string) => {
  return useQuery<ConversationDetail, Error>({
    queryKey: conversationKeys.detail(conversationId),
    queryFn: () => {
      if (!conversationId) throw new Error('Missing conversationId')
      return getConversationDetail(conversationId)
    },
    enabled: Boolean(conversationId),
    staleTime: 30_000,
    retry: false
  })
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

export const useDirectConversationLookupQuery = (recipientId?: string | null) => {
  return useQuery({
    queryKey: conversationKeys.directLookup(recipientId || undefined),
    queryFn: () => {
      if (!recipientId) throw new Error('Missing recipientId')
      return lookupDirectConversation(recipientId)
    },
    enabled: Boolean(recipientId),
    staleTime: 10_000,
    retry: false
  })
}

export const useCreateGroupMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: CreateGroupBody): Promise<GroupConversation> => {
      const response = await conversationApi.createGroup(body)
      return response.data.content
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['conversations', 'list'] })
    }
  })
}
