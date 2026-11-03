import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { ChatMessagesArea } from '~/components/chat-layout/chat-messages-area'
import { messageKeys } from '~/hooks/use-realtime-message-sync'
import type { Message } from '~/types/message.type'

const message = (id: string, content: string, createdAt: string): Message => ({
  id,
  conversationId: 'conversation-1',
  senderId: 'user-2',
  sender: { id: 'user-2', fullName: 'Tài' },
  clientMessageId: `client-${id}`,
  content,
  attachments: [],
  createdAt
})

describe('ChatMessagesArea', () => {
  it('hiển thị lịch sử theo thứ tự cũ đến mới và không lặp tin ở ranh giới trang', () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(messageKeys.list('conversation-1'), {
      pages: [
        {
          data: [message('m3', 'Tin mới', '2026-10-27T10:03:00Z'), message('m2', 'Tin giữa', '2026-10-27T10:02:00Z')],
          nextCursor: 'm2',
          hasMore: true
        },
        {
          data: [message('m2', 'Tin giữa', '2026-10-27T10:02:00Z'), message('m1', 'Tin cũ', '2026-10-27T10:01:00Z')],
          nextCursor: null,
          hasMore: false
        }
      ],
      pageParams: [undefined, 'm2']
    })

    const html = renderToStaticMarkup(
      createElement(
        QueryClientProvider,
        { client: queryClient },
        createElement(ChatMessagesArea, { conversationId: 'conversation-1', title: 'Tài' })
      )
    )

    expect(html.indexOf('Tin cũ')).toBeLessThan(html.indexOf('Tin giữa'))
    expect(html.indexOf('Tin giữa')).toBeLessThan(html.indexOf('Tin mới'))
    expect(html.match(/Tin giữa/g)).toHaveLength(1)
  })

  it('cung cấp liên kết tải tệp đính kèm qua HTTP-19', () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(messageKeys.list('conversation-1'), {
      pages: [
        {
          data: [
            {
              ...message('m1', '', '2026-10-27T10:01:00Z'),
              attachments: [
                {
                  id: 'file-1',
                  name: 'ghi-chu.pdf',
                  contentType: 'application/pdf',
                  size: 128,
                  downloadUrl: '/api/files/file-1'
                }
              ]
            }
          ],
          nextCursor: null,
          hasMore: false
        }
      ],
      pageParams: [undefined]
    })

    const html = renderToStaticMarkup(
      createElement(
        QueryClientProvider,
        { client: queryClient },
        createElement(ChatMessagesArea, { conversationId: 'conversation-1', title: 'Tài' })
      )
    )

    expect(html).toContain('href="/api/files/file-1"')
    expect(html).toContain('download=""')
  })

  it('hiển thị trạng thái đang gửi và lỗi cụ thể của từng tin nhắn', () => {
    const queryClient = new QueryClient()
    const html = renderToStaticMarkup(
      createElement(
        QueryClientProvider,
        { client: queryClient },
        createElement(ChatMessagesArea, {
          conversationId: 'conversation-1',
          title: 'Tài',
          loadHistory: false,
          optimisticMessages: [
            message('pending', 'Đang gửi', '2026-11-02T10:00:00Z'),
            message('failed', 'Bị lỗi', '2026-11-02T10:00:01Z')
          ],
          messageDeliveryStates: {
            'client-pending': { status: 'pending' },
            'client-failed': { status: 'failed', error: 'Bạn không thuộc hội thoại' }
          }
        })
      )
    )

    expect(html).toContain('Đang gửi...')
    expect(html).toContain('Bạn không thuộc hội thoại')
    expect(html).toContain('Thử lại')
  })
})
