import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { ApiPage } from '~/types/api-response.type'
import type { ConversationDetail, ConversationMember, ConversationSummary } from '~/types/conversation.type'
import type { PresenceChangedPayload, ReadReceiptPayload } from '~/types/websocket.type'

interface UsePresenceSyncOptions {
  activeConversationId?: string
}

export const usePresenceSync = ({ activeConversationId }: UsePresenceSyncOptions = {}) => {
  const queryClient = useQueryClient()
  const currentUser = useAuthStore((state) => state.user)
  const currentUserId = currentUser?.id

  const activeConversationIdRef = useRef(activeConversationId)
  const currentUserIdRef = useRef(currentUserId)

  useEffect(() => {
    activeConversationIdRef.current = activeConversationId
    currentUserIdRef.current = currentUserId
  })

  useEffect(() => {
    // 1. Lắng nghe WS-06 PRESENCE_CHANGED để cập nhật trạng thái Online/Offline realtime
    const unsubscribePresence = wsClient.on('PRESENCE_CHANGED', (payload: PresenceChangedPayload) => {
      if (!payload || !payload.userId) return

      // A. Cập nhật danh sách cuộc trò chuyện ['conversations', 'list']
      queryClient.setQueriesData<ApiPage<ConversationSummary[]>>({ queryKey: ['conversations', 'list'] }, (old) => {
        if (!old || !Array.isArray(old.data)) return old

        let changed = false
        const updated = old.data.map((conv) => {
          if (conv.type === 'DIRECT' && conv.peer?.id === payload.userId) {
            changed = true
            return {
              ...conv,
              peer: {
                ...conv.peer,
                fullName: payload.user?.fullName ?? conv.peer.fullName,
                avatarFileId: payload.user?.avatarFileId ?? conv.peer.avatarFileId,
                status: payload.status,
                lastSeen: payload.lastSeen
              }
            }
          }
          return conv
        })

        return changed ? { ...old, data: updated } : old
      })

      // B. Cập nhật chi tiết cuộc trò chuyện ['conversations', 'detail']
      queryClient.setQueriesData<ConversationDetail>({ queryKey: ['conversations', 'detail'] }, (detail) => {
        if (!detail) return detail
        if (detail.type === 'DIRECT' && detail.peer?.id === payload.userId) {
          return {
            ...detail,
            peer: {
              ...detail.peer,
              fullName: payload.user?.fullName ?? detail.peer.fullName,
              avatarFileId: payload.user?.avatarFileId ?? detail.peer.avatarFileId,
              status: payload.status,
              lastSeen: payload.lastSeen
            }
          }
        }
        return detail
      })

      // C. Cập nhật danh sách thành viên nhóm ['group-members']
      queryClient.setQueriesData<ApiPage<ConversationMember[]>>({ queryKey: ['group-members'] }, (old) => {
        if (!old || !Array.isArray(old.data)) return old

        let changed = false
        const updated = old.data.map((member) => {
          if (member.user?.id === payload.userId) {
            changed = true
            return {
              ...member,
              user: {
                ...member.user,
                fullName: payload.user?.fullName ?? member.user.fullName,
                avatarFileId: payload.user?.avatarFileId ?? member.user.avatarFileId,
                status: payload.status,
                lastSeen: payload.lastSeen
              }
            }
          }
          return member
        })

        return changed ? { ...old, data: updated } : old
      })

      // D. Cập nhật trạng thái của chính người dùng hiện tại nếu có thông báo
      if (payload.userId === currentUserIdRef.current) {
        const stateUser = useAuthStore.getState().user
        if (stateUser && stateUser.id === payload.userId && stateUser.status !== payload.status) {
          useAuthStore.getState().login({
            ...stateUser,
            status: payload.status
          })
        }
      }
    })

    // 2. Lắng nghe WS-05 READ_RECEIPT
    const unsubscribeReadReceipt = wsClient.on('READ_RECEIPT', (payload: ReadReceiptPayload) => {
      if (!payload || !payload.conversationId) return

      // Nếu người gửi read receipt là user hiện tại (ví dụ đọc ở tab khác hoặc thiết bị khác)
      if (payload.userId === currentUserIdRef.current) {
        queryClient.setQueriesData<ApiPage<ConversationSummary[]>>({ queryKey: ['conversations', 'list'] }, (old) => {
          if (!old || !Array.isArray(old.data)) return old

          let changed = false
          const updated = old.data.map((conv) => {
            if (conv.id === payload.conversationId && conv.unreadCount !== 0) {
              changed = true
              return {
                ...conv,
                unreadCount: 0
              }
            }
            return conv
          })

          return changed ? { ...old, data: updated } : old
        })
      }
    })

    // 3. Khi WebSocket kết nối lại hoặc thay đổi trạng thái, làm mới danh sách hội thoại và cập nhật presence
    const unsubscribeStatus = wsClient.onStatusChange((status) => {
      if (status === 'OPEN') {
        queryClient.invalidateQueries({ queryKey: ['conversations', 'list'] })
        const stateUser = useAuthStore.getState().user
        if (stateUser && stateUser.status !== 'ONLINE') {
          useAuthStore.getState().login({ ...stateUser, status: 'ONLINE' })
        }
      } else if (status === 'CLOSED') {
        const stateUser = useAuthStore.getState().user
        if (stateUser && stateUser.status !== 'OFFLINE') {
          useAuthStore.getState().login({ ...stateUser, status: 'OFFLINE' })
        }
      }
    })

    return () => {
      unsubscribePresence()
      unsubscribeReadReceipt()
      unsubscribeStatus()
    }
  }, [queryClient])
}
