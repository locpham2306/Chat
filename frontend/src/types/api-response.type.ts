export interface ApiResponse<T> {
  code: string
  strCode: string
  message: string
  content: T
}

export interface ApiPage<T> {
  data: T
  totalPages: number
  totalElements: number
}
