import { create } from 'zustand'
import type { WebSocketStatus } from '~/types/websocket.type'

interface WebSocketState {
  status: WebSocketStatus
  setStatus: (status: WebSocketStatus) => void
}

export const useWebSocketStore = create<WebSocketState>((set) => ({
  status: 'CLOSED',
  setStatus: (status) => set({ status })
}))
