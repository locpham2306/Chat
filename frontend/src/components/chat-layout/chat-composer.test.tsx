import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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

  it('announces failed attachments once with their file names and recovery guidance', () => {
    mockUseFileAttachments.mockReturnValue({
      attachments: [
        {
          localId: 'local-file-1',
          source: new File(['ảnh'], 'anh-loi.png', { type: 'image/png' }),
          progress: 0,
          status: 'failed',
          error: 'Mất kết nối',
          canRetry: true
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

    render(<ChatComposer />)

    expect(screen.getAllByRole('alert')).toHaveLength(1)
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Không thể tải anh-loi.png. Xóa hoặc tải lại tệp lỗi trước khi gửi tin.'
    )
  })

  it('labels the message field and exposes its keyboard shortcuts', () => {
    mockUseFileAttachments.mockReturnValue({
      attachments: [],
      uploadedFiles: [],
      hasUploading: false,
      hasFailed: false,
      addFiles: vi.fn(),
      retry: vi.fn(),
      remove: vi.fn(),
      clear: vi.fn()
    })

    render(<ChatComposer />)

    const messageField = screen.getByRole('textbox', { name: 'Nội dung tin nhắn' })
    expect(messageField).toHaveAttribute('aria-keyshortcuts', 'Enter Shift+Enter')
    expect(messageField).toHaveAccessibleDescription(/Enter.*gửi.*Shift \+ Enter.*xuống dòng/i)
  })

  it('sends with Enter but keeps a newline with Shift+Enter', async () => {
    const onSendMessage = vi.fn(() => true)
    mockUseFileAttachments.mockReturnValue({
      attachments: [],
      uploadedFiles: [],
      hasUploading: false,
      hasFailed: false,
      addFiles: vi.fn(),
      retry: vi.fn(),
      remove: vi.fn(),
      clear: vi.fn()
    })
    render(<ChatComposer onSendMessage={onSendMessage} />)
    const messageField = screen.getByRole('textbox', { name: 'Nội dung tin nhắn' })

    await userEvent.type(messageField, 'Dòng một{shift>}{enter}{/shift}Dòng hai')
    expect(onSendMessage).not.toHaveBeenCalled()
    await userEvent.type(messageField, '{enter}')

    expect(onSendMessage).toHaveBeenCalledWith({ content: 'Dòng một\nDòng hai', attachments: [] })
  })
})
