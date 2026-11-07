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

export interface WebSocketClientOptions {
  url?: string
  minReconnectDelay?: number
  maxReconnectDelay?: number
  reconnectFactor?: number
  jitter?: number
  heartbeatInterval?: number
}

type GenericListener = (data: unknown) => void
type StatusListener = (status: WebSocketStatus) => void
type RawMessageListener = (frame: WebSocketFrame) => void

export class WebSocketClient {
  private static instance: WebSocketClient | null = null

  private socket: WebSocket | null = null
  private status: WebSocketStatus = 'CLOSED'
  private explicitClose = false
  private reconnectAttempts = 0
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null

  private readonly minReconnectDelay: number
  private readonly maxReconnectDelay: number
  private readonly reconnectFactor: number
  private readonly jitter: number
  private readonly heartbeatInterval: number
  private customUrl?: string

  private readonly listeners = new Map<string, Set<GenericListener>>()
  private readonly statusListeners = new Set<StatusListener>()
  private readonly rawMessageListeners = new Set<RawMessageListener>()

  constructor(options: WebSocketClientOptions = {}) {
    this.minReconnectDelay = options.minReconnectDelay ?? 1000
    this.maxReconnectDelay = options.maxReconnectDelay ?? 30000
    this.reconnectFactor = options.reconnectFactor ?? 2
    this.jitter = options.jitter ?? 0.2
    this.heartbeatInterval = options.heartbeatInterval ?? 30000
    this.customUrl = options.url
  }

  public static getInstance(options?: WebSocketClientOptions): WebSocketClient {
    if (!WebSocketClient.instance) {
      WebSocketClient.instance = new WebSocketClient(options)
    }
    return WebSocketClient.instance
  }

  /**
   * Khởi tạo kết nối WebSocket với cookie phiên tự động (không truyền token trên URL)
   */
  public connect(url?: string): void {
    if (url) {
      this.customUrl = url
    }

    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return
    }

    this.explicitClose = false
    this.clearReconnectTimer()
    this.updateStatus('CONNECTING')

    const targetUrl = this.customUrl || getWebSocketUrl()

    try {
      this.socket = new WebSocket(targetUrl)
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
    this.stopHeartbeat()

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

  /**
   * Đăng ký lắng nghe tất cả các frame nhận được
   */
  public onAnyMessage(listener: RawMessageListener): () => void {
    this.rawMessageListeners.add(listener)
    return () => {
      this.rawMessageListeners.delete(listener)
    }
  }

  public getStatus(): WebSocketStatus {
    return this.status
  }

  public isConnected(): boolean {
    return this.status === 'OPEN'
  }

  private setupSocketHandlers(): void {
    if (!this.socket) return

    this.socket.onopen = () => {
      this.reconnectAttempts = 0
      this.updateStatus('OPEN')
      this.startHeartbeat()
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

      // Thông báo cho raw listeners
      this.rawMessageListeners.forEach((listener) => {
        try {
          listener(parsed)
        } catch (err) {
          console.error('[WebSocket] Lỗi trong raw listener:', err)
        }
      })

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
    this.stopHeartbeat()
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

  private startHeartbeat(): void {
    this.stopHeartbeat()
    if (this.heartbeatInterval <= 0) return

    this.heartbeatTimer = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        // Kiểm tra kết nối liveness
      }
    }, this.heartbeatInterval)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
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

export const wsClient = WebSocketClient.getInstance()

// Tự động đồng bộ trạng thái kết nối và lỗi frame vào Zustand store
if (typeof window !== 'undefined') {
  wsClient.onStatusChange((status) => {
    useWebSocketStore.getState().setStatus(status)
  })
  wsClient.on('ERROR', (error) => {
    useWebSocketStore.getState().setLastError(error)
  })
}
