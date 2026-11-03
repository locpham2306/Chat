import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { conversationKeys } from '~/hooks/use-conversation'
import { ChatPage } from '~/pages/chat-page'
import type { User } from '~/types/user.type'

const renderChatPage = (
  initialEntry: string | { pathname: string; search: string; state?: unknown },
  seedQuery?: (queryClient: QueryClient) => void
) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } }
  })
  seedQuery?.(queryClient)

  return renderToStaticMarkup(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path='/chat' element={<ChatPage />} />
          <Route path='/chat/:conversationId' element={<ChatPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('ChatPage direct conversation flow', () => {
  it('hiển thị nút mở tìm kiếm người dùng trong danh sách hội thoại', () => {
    const html = renderChatPage('/chat')

    expect(html).toContain('aria-label="Tìm người dùng"')
  })

  it('hiển thị hội thoại nháp khi chưa có conversationId', () => {
    const recipient: User = {
      id: 'recipient-1',
      username: 'an',
      fullName: 'Nguyễn Văn An',
      avatarFileId: null,
      status: 'OFFLINE',
      lastSeen: null
    }
    const html = renderChatPage(
      {
        pathname: '/chat',
        search: '?recipientId=recipient-1',
        state: { recipient }
      },
      (queryClient) =>
        queryClient.setQueryData(conversationKeys.directLookup('recipient-1'), {
          kind: 'new',
          recipientId: 'recipient-1',
          peer: recipient
        })
    )

    expect(html).toContain('Nguyễn Văn An')
    expect(html).toContain('Hội thoại sẽ được tạo khi gửi tin nhắn đầu tiên.')
  })
})
