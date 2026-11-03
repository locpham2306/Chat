import { describe, expect, expectTypeOf, it } from 'vitest'
import type { ApiError, ApiErrorCode, ApiErrorStatus } from './api-error.type'
import type { ApiPage, ApiResponse } from './api-response.type'
import type { Conversation, ConversationSummary } from './conversation.type'
import type { File } from './file.type'
import type { Message } from './message.type'
import type { CursorPage } from './pagination.type'
import type { User, UserStatus } from './user.type'

describe('kiểu dữ liệu API dùng chung', () => {
  it('giữ đúng union cho trạng thái người dùng và mã lỗi', () => {
    expectTypeOf<User['status']>().toEqualTypeOf<UserStatus>()
    expectTypeOf<ApiError['status']>().toEqualTypeOf<ApiErrorStatus>()
    expectTypeOf<ApiError['code']>().toEqualTypeOf<ApiErrorCode>()
  })

  it('phân biệt hội thoại trực tiếp và nhóm bằng trường type', () => {
    const directConversation = {
      id: 'c1',
      type: 'DIRECT',
      memberIds: ['u1', 'u2'],
      createdAt: '2026-10-20T08:00:00Z'
    } satisfies Conversation
    const groupConversation = {
      id: 'c2',
      type: 'GROUP',
      name: 'Nhóm BTL',
      avatarFileId: null,
      ownerId: 'u1',
      memberCount: 2,
      createdAt: '2026-10-20T08:00:00Z'
    } satisfies Conversation

    expect(directConversation.type).toBe('DIRECT')
    expect(groupConversation.type).toBe('GROUP')
  })

  it('liên kết message, file và envelope pagination đúng hợp đồng', () => {
    expectTypeOf<Message['attachments']>().toEqualTypeOf<File[]>()
    expectTypeOf<ApiResponse<User>['content']>().toEqualTypeOf<User>()
    expectTypeOf<ApiPage<User[]>['data']>().toEqualTypeOf<User[]>()
    expectTypeOf<CursorPage<Message>['nextCursor']>().toEqualTypeOf<string | null>()
    expectTypeOf<ConversationSummary['lastMessage']>().toEqualTypeOf<Message | null>()
  })
})
