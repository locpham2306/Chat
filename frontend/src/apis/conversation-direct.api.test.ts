import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import conversationApi from '~/apis/conversation.api'
import http from '~/lib/http'

const originalAdapter = http.defaults.adapter

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

describe('conversationApi.findDirectConversation', () => {
  it('gọi HTTP-08 với recipientId và đọc hội thoại đã tồn tại', async () => {
    let receivedConfig: InternalAxiosRequestConfig | undefined
    http.defaults.adapter = async (config) => {
      receivedConfig = config
      return {
        data: {
          code: '0',
          strCode: 'success',
          message: 'SUCCESSFUL',
          content: {
            id: 'conversation-1',
            type: 'DIRECT',
            memberIds: ['current-user', 'recipient-1'],
            createdAt: '2026-10-26T00:00:00Z'
          }
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config
      } satisfies AxiosResponse
    }

    const response = await conversationApi.findDirectConversation('recipient-1')

    expect(receivedConfig?.url).toBe('/api/conversations/direct')
    expect(receivedConfig?.params).toEqual({ recipientId: 'recipient-1' })
    expect(response.status).toBe(200)
    const content = response.data?.content
    expect(content).toBeDefined()
    if (!content || !('id' in content)) throw new Error('Expected an existing direct conversation')
    expect(content.id).toBe('conversation-1')
  })

  it('giữ nguyên response 204 khi chưa có hội thoại', async () => {
    http.defaults.adapter = async (config) =>
      ({
        data: undefined,
        status: 204,
        statusText: 'No Content',
        headers: {},
        config
      }) satisfies AxiosResponse

    const response = await conversationApi.findDirectConversation('recipient-2')

    expect(response.status).toBe(204)
    expect(response.data).toBeUndefined()
  })
})

describe('conversationApi.getConversations', () => {
  it('đọc danh sách HTTP-10 trong ApiResponse<ApiPage>', async () => {
    let requestConfig: InternalAxiosRequestConfig | undefined
    http.defaults.adapter = async (config) => {
      requestConfig = config
      return {
        data: {
          code: '0',
          strCode: 'success',
          message: 'SUCCESSFUL',
          content: {
            data: [],
            totalPages: 0,
            totalElements: 0
          }
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config
      } satisfies AxiosResponse
    }

    const response = await conversationApi.getConversations({ page: 0, size: 20 })

    expect(requestConfig?.method).toBe('get')
    expect(requestConfig?.url).toBe('/api/conversations')
    expect(requestConfig?.params).toEqual({ page: 0, size: 20 })
    expect(response.data.content).toEqual({ data: [], totalPages: 0, totalElements: 0 })
  })
})
