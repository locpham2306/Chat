import http from '~/lib/http'
import type { ApiResponse } from '~/types/api-response.type'
import type { MessageHistoryPage } from '~/types/message.type'

export interface GetMessageHistoryParams {
  conversationId: string
  before?: string
  limit?: number
  signal?: AbortSignal
}

const messageApi = {
  getHistory: ({ conversationId, before, limit = 30, signal }: GetMessageHistoryParams) =>
    http.get<ApiResponse<MessageHistoryPage>>(`/api/conversations/${conversationId}/messages`, {
      params: { ...(before ? { before } : {}), limit },
      signal
    })
}

export default messageApi
