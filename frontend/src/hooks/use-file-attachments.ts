import axios from 'axios'
import { useCallback, useState } from 'react'
import { v4 as uuidv4 } from 'uuid'
import fileApi from '~/apis/file.api'
import { getApiErrorMessage } from '~/lib/api-error'
import type { File as UploadedFile } from '~/types/file.type'

const MAX_FILE_SIZE = 20 * 1024 * 1024

export interface FileAttachmentUpload {
  localId: string
  source: globalThis.File
  uploadedFile?: UploadedFile
  progress: number
  status: 'uploading' | 'ready' | 'failed'
  error?: string
  canRetry?: boolean
}

export const getAttachmentValidationError = (file: globalThis.File): string | undefined =>
  file.size === 0
    ? 'Không thể tải lên tệp rỗng.'
    : file.size > MAX_FILE_SIZE
      ? 'Dung lượng mỗi tệp không được vượt quá 20 MiB.'
      : undefined

const getUploadErrorMessage = (error: unknown) => {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 413) return 'Tệp vượt quá dung lượng cho phép 20 MiB.'
    if (error.response?.status === 415) return 'Định dạng tệp không được hỗ trợ.'
  }
  return getApiErrorMessage(error) || 'Không thể tải tệp lên. Vui lòng thử lại.'
}

const canRetryUpload = (error: unknown) =>
  !axios.isAxiosError(error) || (error.response?.status !== 413 && error.response?.status !== 415)

export const useFileAttachments = () => {
  const [attachments, setAttachments] = useState<FileAttachmentUpload[]>([])

  const upload = useCallback(async (localId: string, file: globalThis.File) => {
    try {
      const response = await fileApi.upload(file, (progress) => {
        setAttachments((current) => current.map((item) => (item.localId === localId ? { ...item, progress } : item)))
      })
      setAttachments((current) =>
        current.map((item) =>
          item.localId === localId
            ? { ...item, progress: 100, status: 'ready', uploadedFile: response.data.content, error: undefined }
            : item
        )
      )
    } catch (error) {
      setAttachments((current) =>
        current.map((item) =>
          item.localId === localId
            ? { ...item, status: 'failed', error: getUploadErrorMessage(error), canRetry: canRetryUpload(error) }
            : item
        )
      )
    }
  }, [])

  const addFiles = useCallback(
    (files: globalThis.File[]) => {
      const nextItems = files.map((file) => {
        const validationError = getAttachmentValidationError(file)
        return {
          localId: uuidv4(),
          source: file,
          progress: 0,
          status: validationError ? ('failed' as const) : ('uploading' as const),
          ...(validationError ? { error: validationError, canRetry: false } : {})
        }
      })

      setAttachments((current) => [...current, ...nextItems])
      nextItems.filter((item) => item.status === 'uploading').forEach((item) => void upload(item.localId, item.source))
    },
    [upload]
  )

  const retry = useCallback(
    (localId: string) => {
      const attachment = attachments.find((item) => item.localId === localId)
      if (!attachment || getAttachmentValidationError(attachment.source)) return
      setAttachments((current) =>
        current.map((item) =>
          item.localId === localId
            ? { ...item, progress: 0, status: 'uploading', error: undefined, canRetry: undefined }
            : item
        )
      )
      void upload(localId, attachment.source)
    },
    [attachments, upload]
  )

  const remove = useCallback((localId: string) => {
    setAttachments((current) => current.filter((item) => item.localId !== localId))
  }, [])

  const clear = useCallback(() => setAttachments([]), [])
  const uploadedFiles = attachments.flatMap((item) =>
    item.status === 'ready' && item.uploadedFile ? [item.uploadedFile] : []
  )

  return {
    attachments,
    uploadedFiles,
    hasUploading: attachments.some((item) => item.status === 'uploading'),
    hasFailed: attachments.some((item) => item.status === 'failed'),
    addFiles,
    retry,
    remove,
    clear
  }
}
