import { act, renderHook, waitFor } from '@testing-library/react'
import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import { getAttachmentValidationError, useFileAttachments } from './use-file-attachments'
import http from '~/lib/http'

const originalAdapter = http.defaults.adapter
const uploadedFile = {
  id: 'f1',
  originalName: 'document.txt',
  contentType: 'text/plain',
  size: 4,
  downloadUrl: '/api/files/f1'
}

const successResponse = (config: InternalAxiosRequestConfig): AxiosResponse => ({
  data: { content: uploadedFile },
  status: 200,
  statusText: 'OK',
  headers: {},
  config
})

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

describe('file attachments', () => {
  it('rejects empty and oversized files before upload', () => {
    const empty = new File([], 'empty.txt')
    const oversized = new File(['x'], 'large.bin')
    Object.defineProperty(oversized, 'size', { value: 20 * 1024 * 1024 + 1 })

    expect(getAttachmentValidationError(empty)).toBe('Không thể tải lên tệp rỗng.')
    expect(getAttachmentValidationError(oversized)).toBe('Dung lượng mỗi tệp không được vượt quá 20 MiB.')
  })

  it('stays loading until upload succeeds and then exposes the uploaded file', async () => {
    let resolveUpload!: () => void
    http.defaults.adapter = (config) =>
      new Promise((resolve) => {
        resolveUpload = () => resolve(successResponse(config))
      })
    const { result } = renderHook(() => useFileAttachments())

    act(() => result.current.addFiles([new File(['data'], 'document.txt')]))
    await waitFor(() => expect(result.current.attachments[0]?.status).toBe('uploading'))

    resolveUpload()
    await waitFor(() => expect(result.current.attachments[0]?.status).toBe('ready'))
    expect(result.current.attachments[0]).toMatchObject({ progress: 100, uploadedFile })
    expect(result.current.uploadedFiles).toEqual([uploadedFile])
  })

  it('shows a non-retryable message for unsupported media type', async () => {
    http.defaults.adapter = async (config) => {
      const response = {
        data: {},
        status: 415,
        statusText: 'Unsupported Media Type',
        headers: {},
        config
      } satisfies AxiosResponse
      throw new AxiosError('Unsupported', AxiosError.ERR_BAD_REQUEST, config, undefined, response)
    }
    const { result } = renderHook(() => useFileAttachments())

    act(() => result.current.addFiles([new File(['data'], 'virus.exe')]))

    await waitFor(() => expect(result.current.attachments[0]?.status).toBe('failed'))
    expect(result.current.attachments[0]).toMatchObject({
      error: 'Định dạng tệp không được hỗ trợ.',
      canRetry: false
    })
  })

  it('retries a temporary upload failure with the original file', async () => {
    const uploadedSources: unknown[] = []
    let attempts = 0
    http.defaults.adapter = async (config) => {
      attempts++
      uploadedSources.push((config.data as FormData).get('file'))
      if (attempts === 1) throw new AxiosError('Network Error', AxiosError.ERR_NETWORK, config)
      return successResponse(config)
    }
    const source = new File(['data'], 'document.txt')
    const { result } = renderHook(() => useFileAttachments())

    act(() => result.current.addFiles([source]))
    await waitFor(() => expect(result.current.attachments[0]?.canRetry).toBe(true))

    act(() => result.current.retry(result.current.attachments[0].localId))
    await waitFor(() => expect(result.current.attachments[0]?.status).toBe('ready'))
    expect(uploadedSources).toEqual([source, source])
  })
})
