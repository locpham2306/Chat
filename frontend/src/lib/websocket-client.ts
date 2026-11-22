import { getWebSocketUrl } from './config'
import { useWebSocketStore } from '~/stores/use-websocket-store'
import type {
  ClientEvent,
  ClientEventMap,
  ServerEvent,
  ServerEventMap,
  WebSocketFrame,
  WebSocketListener,
  WebSocketStatus
} from '~/types/websocket.type'

type GenericListener = (data: unknown) => void
type StatusListener = (status: WebSocketStatus) => void

class WebSocketClient {
  private socket: WebSocket | null = null
  private status: WebSocketStatus = 'CLOSED'
  private explicitClose = false
  private reconnectAttempts = 0
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null

  private readonly minReconnectDelay = 1000
  private readonly maxReconnectDelay = 30000
  private readonly reconnectFactor = 2
  private readonly jitter = 0.2

  private readonly listeners = new Map<string, Set<GenericListener>>()
  private readonly statusListeners = new Set<StatusListener>()

  /**
   * Khởi tạo kết nối WebSocket với cookie phiên tự động (không truyền token trên URL)
   */
  public connect(): void {
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return
    }

    this.explicitClose = false
    this.clearReconnectTimer()
    this.updateStatus('CONNECTING')

    try {
      this.socket = new WebSocket(getWebSocketUrl())
      this.setupSocketHandlers()
    } catch (error) {
      console.error('[WebSocket] Lỗi khi tạo WebSocket:', error)
      this.handleSocketClose()
    }
  }

  /**
   * Đóng kết nối có chủ đích (ví dụ khi người dùng đăng xuất)
   */
  public disconnect(): void {
    this.explicitClose = true
    this.clearReconnectTimer()

    if (this.socket) {
      // Hủy bỏ handlers để tránh trigger reconnect ngoài ý muốn
      this.socket.onopen = null
      this.socket.onmessage = null
      this.socket.onerror = null
      this.socket.onclose = null

      if (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING) {
        this.socket.close()
      }
      this.socket = null
    }

    this.reconnectAttempts = 0
    this.updateStatus('CLOSED')
  }

  /**
   * Gửi frame dữ liệu JSON lên server
   */
  public send<E extends ClientEvent>(event: E, data: ClientEventMap[E]): boolean {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      console.warn(`[WebSocket] Không thể gửi event '${event}' vì kết nối chưa sẵn sàng (status: ${this.status})`)
      return false
    }

    const frame: WebSocketFrame<E, ClientEventMap[E]> = { event, data }
    try {
      this.socket.send(JSON.stringify(frame))
      return true
    } catch (error) {
      console.error(`[WebSocket] Lỗi khi gửi event '${event}':`, error)
      return false
    }
  }

  /**
   * Đăng ký lắng nghe sự kiện cụ thể từ Server
   */
  public on<E extends ServerEvent>(event: E, listener: WebSocketListener<ServerEventMap[E]>): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())
    }
    const set = this.listeners.get(event)!
    set.add(listener as GenericListener)

    return () => {
      this.off(event, listener)
    }
  }

  /**
   * Hủy đăng ký lắng nghe sự kiện cụ thể
   */
  public off<E extends ServerEvent>(event: E, listener: WebSocketListener<ServerEventMap[E]>): void {
    const set = this.listeners.get(event)
    if (set) {
      set.delete(listener as GenericListener)
      if (set.size === 0) {
        this.listeners.delete(event)
      }
    }
  }

  /**
   * Đăng ký lắng nghe thay đổi trạng thái kết nối
   */
  public onStatusChange(listener: StatusListener): () => void {
    this.statusListeners.add(listener)
    listener(this.status)

    return () => {
      this.statusListeners.delete(listener)
    }
  }

  private setupSocketHandlers(): void {
    if (!this.socket) return

    this.socket.onopen = () => {
      this.reconnectAttempts = 0
      this.updateStatus('OPEN')
    }

    this.socket.onmessage = (event: MessageEvent) => {
      this.handleIncomingMessage(event.data)
    }

    this.socket.onerror = (error: Event) => {
      console.error('[WebSocket] Socket gặp lỗi:', error)
    }

    this.socket.onclose = () => {
      this.handleSocketClose()
    }
  }

  private handleIncomingMessage(rawPayload: unknown): void {
    if (typeof rawPayload !== 'string') {
      return
    }

    try {
      const parsed = JSON.parse(rawPayload) as WebSocketFrame
      if (!parsed || typeof parsed.event !== 'string') {
        return
      }

      // Điều hướng tới event listener tương ứng
      const handlers = this.listeners.get(parsed.event)
      if (handlers && handlers.size > 0) {
        handlers.forEach((handler) => {
          try {
            handler(parsed.data)
          } catch (err) {
            console.error(`[WebSocket] Lỗi xử lý listener cho event '${parsed.event}':`, err)
          }
        })
      }
    } catch {
      // Bỏ qua nếu tin nhắn không phải JSON hợp lệ
    }
  }

  private handleSocketClose(): void {
    this.socket = null
    this.updateStatus('CLOSED')

    if (!this.explicitClose) {
      this.scheduleReconnect()
    }
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimer()

    // Tính toán Exponential Backoff: min * (factor ^ attempts)
    const baseDelay = Math.min(
      this.minReconnectDelay * Math.pow(this.reconnectFactor, this.reconnectAttempts),
      this.maxReconnectDelay
    )

    // Jitter: dao động ngẫu nhiên quanh baseDelay để tránh nghẽn đồng loạt
    const jitterOffset = baseDelay * this.jitter * (Math.random() * 2 - 1)
    const delay = Math.max(this.minReconnectDelay, Math.round(baseDelay + jitterOffset))

    this.reconnectAttempts++

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  private updateStatus(nextStatus: WebSocketStatus): void {
    if (this.status === nextStatus) return
    this.status = nextStatus
    this.statusListeners.forEach((listener) => {
      try {
        listener(nextStatus)
      } catch (err) {
        console.error('[WebSocket] Lỗi trong status listener:', err)
      }
    })
  }
}

export const wsClient = new WebSocketClient()

// Đồng bộ trạng thái kết nối vào Zustand store
if (typeof window !== 'undefined') {
  wsClient.onStatusChange((status) => {
    useWebSocketStore.getState().setStatus(status)
  })
}
