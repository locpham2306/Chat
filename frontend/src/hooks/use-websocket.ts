import { useEffect, useRef } from 'react'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import type { ServerEvent, ServerEventMap, WebSocketListener } from '~/types/websocket.type'

/**
 * Hook đăng ký lắng nghe sự kiện cụ thể từ Server, tự động dọn dẹp khi unmount
 */
export const useWebSocketEvent = <E extends ServerEvent>(
  event: E,
  handler: WebSocketListener<ServerEventMap[E]>
): void => {
  const handlerRef = useRef(handler)

  useEffect(() => {
    handlerRef.current = handler
  })

  useEffect(() => {
    const unsubscribe = wsClient.on(event, (data) => {
      handlerRef.current(data)
    })

    return () => {
      unsubscribe()
    }
  }, [event])
}

/**
 * Hook tự động quản lý vòng đời kết nối WebSocket dựa trên trạng thái đăng nhập
 */
export const useWebSocketLifecycle = (): void => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)

  useEffect(() => {
    if (isAuthenticated) {
      wsClient.connect()
    } else {
      wsClient.disconnect()
    }
  }, [isAuthenticated])
}
