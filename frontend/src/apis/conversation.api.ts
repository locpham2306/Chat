import http from '~/lib/http'
import type { ApiResponse } from '~/types/api-response.type'
import type { ConversationSummary, DirectConversation } from '~/types/conversation.type'
import type { Page } from '~/types/pagination.type'

export interface GetConversationsParams {
  page?: number
  size?: number
}

const conversationApi = {
  getConversations: (params?: GetConversationsParams) =>
    http.get<Page<ConversationSummary>>('/api/conversations', { params }),
  findDirectConversation: (recipientId: string) =>
    http.get<ApiResponse<DirectConversation> | undefined>('/api/conversations/direct', {
      params: { recipientId }
    })
}

export default conversationApi
