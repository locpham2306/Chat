import http from '~/lib/http'
import type { AxiosProgressEvent } from 'axios'
import type { ApiResponse } from '~/types/api-response.type'
import type { File as FileResponse } from '~/types/file.type'

const fileApi = {
  upload: (file: Blob | globalThis.File, onProgress?: (percentage: number) => void) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<ApiResponse<FileResponse>>('/api/files', formData, {
      onUploadProgress: (event: AxiosProgressEvent) => {
        if (!event.total) return
        onProgress?.(Math.round((event.loaded / event.total) * 100))
      }
    })
  }
}

export default fileApi
