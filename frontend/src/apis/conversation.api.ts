import http from '~/lib/http'
import type { ApiPage, ApiResponse } from '~/types/api-response.type'
import type {
  AddConversationMembersBody,
  AddConversationMembersResponse,
  ConversationDetail,
  ConversationMember,
  ConversationSummary,
  CreateGroupBody,
  DirectConversation,
  DirectConversationLookupResult,
  GroupConversation,
  MarkConversationReadBody,
  MarkConversationReadResponse,
  UpdateConversationBody
} from '~/types/conversation.type'
import type { User } from '~/types/user.type'

export interface GetConversationsParams {
  page?: number
  size?: number
}

export interface GetGroupMembersParams {
  page?: number
  size?: number
}

export interface GetAvailableMembersParams {
  keyword?: string
  page?: number
  size?: number
}

const conversationApi = {
  getConversations: (params?: GetConversationsParams) =>
    http.get<ApiResponse<ApiPage<ConversationSummary[]>>>('/api/conversations', { params }),
  getConversationDetail: (id: string) => http.get<ApiResponse<ConversationDetail>>(`/api/conversations/${id}`),
  findDirectConversation: (recipientId: string) =>
    http.get<ApiResponse<DirectConversation | DirectConversationLookupResult> | undefined>(
      '/api/conversations/direct',
      {
        params: { recipientId }
      }
    ),
  createGroup: (body: CreateGroupBody) => http.post<ApiResponse<GroupConversation>>('/api/conversations/group', body),
  updateConversation: (id: string, body: UpdateConversationBody) =>
    http.put<ApiResponse<ConversationDetail>>(`/api/conversations/${id}`, body),
  markAsRead: (id: string, body: MarkConversationReadBody) =>
    http.put<ApiResponse<MarkConversationReadResponse>>(`/api/conversations/${id}/read`, body),
  getMembers: (id: string, params?: GetGroupMembersParams) =>
    http.get<ApiResponse<ApiPage<ConversationMember[]>>>(`/api/conversations/${id}/members`, { params }),
  addMembers: (id: string, body: AddConversationMembersBody) =>
    http.post<ApiResponse<AddConversationMembersResponse>>(`/api/conversations/${id}/members`, body),
  removeMember: (id: string, userId: string) => http.delete<void>(`/api/conversations/${id}/members/${userId}`),
  getAvailableMembers: (id: string, params?: GetAvailableMembersParams) =>
    http.get<ApiResponse<ApiPage<User[]>>>(`/api/conversations/${id}/available-members`, { params })
}

export default conversationApi
