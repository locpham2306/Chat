import http from '~/lib/http'
import type { ApiPage, ApiResponse } from '~/types/api-response.type'
import type { ConversationSummary } from '~/types/conversation.type'

export interface GetConversationsParams {
  page?: number
  size?: number
}

const conversationApi = {
  getConversations: (params?: GetConversationsParams) =>
    http.get<ApiResponse<ApiPage<ConversationSummary[]>>>('/api/conversations', { params })
}

export default conversationApi
