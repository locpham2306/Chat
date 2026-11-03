import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'

const mockUseFileAttachments = vi.hoisted(() => vi.fn())

vi.mock('~/hooks/use-file-attachments', () => ({
  useFileAttachments: mockUseFileAttachments
}))

import { ChatComposer } from '~/components/chat-layout/chat-composer'

describe('ChatComposer', () => {
  it('hiển thị lỗi dung lượng tệp và không đề nghị tải lại tệp không hợp lệ', () => {
    mockUseFileAttachments.mockReturnValue({
      attachments: [
        {
          localId: 'local-file-1',
          source: { name: 'qua-gioi-han.pdf', size: 20 * 1024 * 1024 + 1 } as globalThis.File,
          progress: 0,
          status: 'failed',
          error: 'Dung lượng mỗi tệp không được vượt quá 20 MiB.',
          canRetry: false
        }
      ],
      uploadedFiles: [],
      hasUploading: false,
      hasFailed: true,
      addFiles: vi.fn(),
      retry: vi.fn(),
      remove: vi.fn(),
      clear: vi.fn()
    })

    const html = renderToStaticMarkup(createElement(ChatComposer))

    expect(html).toContain('>Dung lượng mỗi tệp không được vượt quá 20 MiB.</span>')
    expect(html).not.toContain('aria-label="Tải lại qua-gioi-han.pdf"')
  })
})
