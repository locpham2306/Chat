import { create } from 'zustand'
import type { WebSocketErrorPayload, WebSocketStatus } from '~/types/websocket.type'

interface WebSocketState {
  status: WebSocketStatus
  isConnected: boolean
  lastError: WebSocketErrorPayload | null
  setStatus: (status: WebSocketStatus) => void
  setLastError: (error: WebSocketErrorPayload | null) => void
  reset: () => void
}

export const useWebSocketStore = create<WebSocketState>((set) => ({
  status: 'CLOSED',
  isConnected: false,
  lastError: null,
  setStatus: (status) => set({ status, isConnected: status === 'OPEN' }),
  setLastError: (lastError) => set({ lastError }),
  reset: () => set({ status: 'CLOSED', isConnected: false, lastError: null })
}))
