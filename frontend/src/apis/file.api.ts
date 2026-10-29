import http from '~/lib/http'
import type { ApiResponse } from '~/types/api-response.type'
import type { File as FileResponse } from '~/types/file.type'

const fileApi = {
  upload: (file: Blob | globalThis.File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<ApiResponse<FileResponse>>('/api/files', formData)
  }
}

export default fileApi
