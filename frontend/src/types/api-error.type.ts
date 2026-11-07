export type ApiErrorStatus = 400 | 401 | 403 | 404 | 409 | 413 | 415 | 500

export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'PAYLOAD_TOO_LARGE'
  | 'UNSUPPORTED_MEDIA_TYPE'
  | 'INTERNAL_ERROR'

export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiError {
  timestamp: string
  status: ApiErrorStatus
  code: ApiErrorCode
  message: string
  path: string
  fieldErrors: ApiFieldError[]
}
