import http from '~/lib/http'
import type { ConversationSummary } from '~/types/conversation.type'
import type { Page } from '~/types/pagination.type'

export interface GetConversationsParams {
  page?: number
  size?: number
}

const conversationApi = {
  getConversations: (params?: GetConversationsParams) =>
    http.get<Page<ConversationSummary>>('/api/conversations', { params })
}

export default conversationApi
