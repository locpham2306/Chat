import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import conversationApi, { type GetAvailableMembersParams, type GetGroupMembersParams } from '~/apis/conversation.api'
import { conversationKeys } from '~/hooks/use-conversation'
import type { ApiPage } from '~/types/api-response.type'
import type { AddConversationMembersResponse, ConversationMember } from '~/types/conversation.type'
import type { User } from '~/types/user.type'

export const groupMemberKeys = {
  all: (conversationId: string) => ['group-members', conversationId] as const,
  list: (conversationId: string, params?: GetGroupMembersParams) =>
    ['group-members', conversationId, 'list', params] as const,
  available: (conversationId: string, params?: GetAvailableMembersParams) =>
    ['group-members', conversationId, 'available', params] as const
}

export const useGroupMembersQuery = (
  conversationId: string,
  params?: GetGroupMembersParams,
  options?: { enabled?: boolean }
) => {
  return useQuery<ApiPage<ConversationMember[]>, Error>({
    queryKey: groupMemberKeys.list(conversationId, params),
    queryFn: async () => {
      const response = await conversationApi.getMembers(conversationId, params)
      return response.data.content
    },
    enabled: Boolean(conversationId) && (options?.enabled ?? true),
    placeholderData: keepPreviousData
  })
}

export const useAvailableMembersQuery = (
  conversationId: string,
  params?: GetAvailableMembersParams,
  options?: { enabled?: boolean }
) => {
  return useQuery<ApiPage<User[]>, Error>({
    queryKey: groupMemberKeys.available(conversationId, params),
    queryFn: async () => {
      const response = await conversationApi.getAvailableMembers(conversationId, params)
      return response.data.content
    },
    enabled: Boolean(conversationId) && (options?.enabled ?? true),
    placeholderData: keepPreviousData
  })
}

export const useAddGroupMembersMutation = (conversationId: string) => {
  const queryClient = useQueryClient()
  return useMutation<AddConversationMembersResponse, Error, string[]>({
    mutationFn: async (memberIds: string[]) => {
      const response = await conversationApi.addMembers(conversationId, { memberIds })
      return response.data.content
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: groupMemberKeys.all(conversationId) })
      void queryClient.invalidateQueries({ queryKey: conversationKeys.detail(conversationId) })
      void queryClient.invalidateQueries({ queryKey: conversationKeys.all })
    }
  })
}

export const useRemoveGroupMemberMutation = (conversationId: string) => {
  const queryClient = useQueryClient()
  return useMutation<void, Error, string>({
    mutationFn: async (userId: string) => {
      await conversationApi.removeMember(conversationId, userId)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: groupMemberKeys.all(conversationId) })
      void queryClient.invalidateQueries({ queryKey: conversationKeys.detail(conversationId) })
      void queryClient.invalidateQueries({ queryKey: conversationKeys.all })
    }
  })
}
