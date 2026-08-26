// WebSocket 封装：与网关 /ws/production 建立连接
// 说明：Token 放 query 参数（WebSocket 无法携带自定义 Header，网关对 query 参数兜底校验）
import { getToken } from './auth'

let ws = null
const listeners = new Map() // eventType -> Set<handler>

function resolveUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const token = encodeURIComponent(getToken())
  return `${protocol}//${window.location.host}/ws/production?token=${token}`
}

export function connectWebSocket() {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return
  ws = new WebSocket(resolveUrl())
  ws.onmessage = (event) => {
    let payload
    try {
      payload = JSON.parse(event.data)
    } catch (e) {
      return
    }
    // 消息格式：{ eventType, data }，与后端 broadcast 约定一致
    const handlers = listeners.get(payload.eventType)
    if (handlers) handlers.forEach((fn) => fn(payload.data))
  }
  ws.onclose = () => {
    ws = null
  }
}

export function disconnectWebSocket() {
  if (ws) {
    ws.close()
    ws = null
  }
}

export function onEvent(eventType, handler) {
  if (!listeners.has(eventType)) listeners.set(eventType, new Set())
  listeners.get(eventType).add(handler)
}

export function offEvent(eventType, handler) {
  const handlers = listeners.get(eventType)
  if (handlers) handlers.delete(handler)
}
