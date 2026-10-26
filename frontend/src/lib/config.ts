export const getWebSocketUrl = (): string => {
  if (import.meta.env.VITE_WS_URL) {
    return import.meta.env.VITE_WS_URL
  }

  if (typeof window !== 'undefined') {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}/ws`
  }

  return 'ws://localhost:8086/ws'
}

export const envConfig = {
  apiUrl: import.meta.env.VITE_API_URL || '',
  timeout: Number(import.meta.env.VITE_TIMEOUT) || 10000,
  wsUrl: getWebSocketUrl()
}
