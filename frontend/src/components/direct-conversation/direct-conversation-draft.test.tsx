import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

vi.mock('~/components/chat-layout', () => ({
  ChatHeader: ({ title }: { title: string }) => <h2>{title}</h2>,
  ChatMessagesArea: () => <div>Tin nhắn</div>,
  ChatComposer: () => <textarea aria-label='Nội dung tin nhắn' />
}))

import { DirectConversationDraft } from './direct-conversation-draft'

describe('DirectConversationDraft accessibility', () => {
  it('exposes a named conversation region', () => {
    render(
      <DirectConversationDraft
        recipientId='u2'
        recipient={{
          id: 'u2',
          username: 'an',
          fullName: 'Nguyễn Văn An',
          avatarFileId: null,
          status: 'ONLINE',
          lastSeen: null
        }}
      />
    )

    expect(screen.getByRole('region', { name: 'Hội thoại với Nguyễn Văn An' })).toBeInTheDocument()
  })
})
