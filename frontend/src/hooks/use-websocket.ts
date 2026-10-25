import { useCallback, useEffect, useRef } from 'react'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import { useWebSocketStore } from '~/stores/use-websocket-store'
import type {
  ClientEvent,
  ClientEventMap,
  ServerEvent,
  ServerEventMap,
  WebSocketListener
} from '~/types/websocket.type'

/**
 * Hook quản lý trạng thái kết nối WebSocket và các thao tác cơ bản
 */
export const useWebSocket = () => {
  const status = useWebSocketStore((state) => state.status)
  const isConnected = useWebSocketStore((state) => state.isConnected)
  const lastError = useWebSocketStore((state) => state.lastError)

  const connect = useCallback((url?: string) => {
    wsClient.connect(url)
  }, [])

  const disconnect = useCallback(() => {
    wsClient.disconnect()
  }, [])

  const send = useCallback(<E extends ClientEvent>(event: E, data: ClientEventMap[E]): boolean => {
    return wsClient.send(event, data)
  }, [])

  return {
    status,
    isConnected,
    lastError,
    connect,
    disconnect,
    send
  }
}

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

    return () => {
      // Khi component vòng đời unmount
    }
  }, [isAuthenticated])
}
