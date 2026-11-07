import { AxiosError, type AxiosResponse } from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import { lookupDirectConversation } from '~/hooks/use-conversation'
import http from '~/lib/http'

const originalAdapter = http.defaults.adapter

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

describe('lookupDirectConversation', () => {
  it('trả conversationId để điều hướng khi DIRECT đã tồn tại', async () => {
    http.defaults.adapter = async (config) =>
      ({
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
      }) satisfies AxiosResponse

    await expect(lookupDirectConversation('recipient-1')).resolves.toEqual({
      kind: 'existing',
      conversationId: 'conversation-1'
    })
  })

  it('giữ recipientId khi HTTP-08 trả 204', async () => {
    http.defaults.adapter = async (config) =>
      ({
        data: undefined,
        status: 204,
        statusText: 'No Content',
        headers: {},
        config
      }) satisfies AxiosResponse

    await expect(lookupDirectConversation('recipient-2')).resolves.toEqual({
      kind: 'new',
      recipientId: 'recipient-2'
    })
  })

  it('giữ nguyên lỗi 403 để giao diện hiển thị trạng thái bị từ chối', async () => {
    http.defaults.adapter = async (config) => {
      const response = {
        data: {},
        status: 403,
        statusText: 'Forbidden',
        headers: {},
        config
      } satisfies AxiosResponse
      throw new AxiosError('Forbidden', AxiosError.ERR_BAD_REQUEST, config, undefined, response)
    }

    let caught: unknown
    try {
      await lookupDirectConversation('recipient-3')
    } catch (error) {
      caught = error
    }

    expect(caught).toMatchObject({ response: { status: 403 } })
  })
})
