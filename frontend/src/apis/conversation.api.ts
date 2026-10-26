import http from '~/lib/http'
import type { ApiPage, ApiResponse } from '~/types/api-response.type'
import type {
  ConversationDetail,
  ConversationSummary,
  DirectConversation,
  DirectConversationLookupResult
} from '~/types/conversation.type'

export interface GetConversationsParams {
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
    )
}

export default conversationApi
