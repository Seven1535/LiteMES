// WebSocket 封装：与网关 /ws/production 建立连接（架构设计 3.6）
// 说明：
// 1. Token 放 query 参数（WebSocket 握手无法携带自定义 Header，网关对 query 参数兜底校验）
// 2. 心跳保活：每 30s 发 ping，服务端回 pong
// 3. 断线重连：指数退避（1s → 2s → 4s ... 上限 30s），重连成功后触发 onReconnected 供页面全量拉取
import { getToken } from './auth'

const HEARTBEAT_INTERVAL = 30 * 1000
const RECONNECT_MAX_DELAY = 30 * 1000

let ws = null
let heartbeatTimer = null
let reconnectTimer = null
let reconnectDelay = 1000
let manualClose = false

const listeners = new Map() // eventType -> Set<handler>
const reconnectHandlers = new Set()
const statusHandlers = new Set()

function resolveUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const token = encodeURIComponent(getToken())
  return `${protocol}//${window.location.host}/ws/production?token=${token}`
}

function notifyStatus(online) {
  statusHandlers.forEach((fn) => fn(online))
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send('ping')
    }
  }, HEARTBEAT_INTERVAL)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function scheduleReconnect() {
  if (manualClose || reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_DELAY)
    connectWebSocket()
  }, reconnectDelay)
}

export function connectWebSocket() {
  manualClose = false
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return
  ws = new WebSocket(resolveUrl())

  ws.onopen = () => {
    reconnectDelay = 1000
    startHeartbeat()
    notifyStatus(true)
  }

  ws.onmessage = (event) => {
    if (event.data === 'pong') return // 心跳应答
    let payload
    try {
      payload = JSON.parse(event.data)
    } catch (e) {
      return
    }
    // 消息格式：{ type, data, timestamp }，与后端 ProductionEvent 约定一致
    const handlers = listeners.get(payload.type)
    if (handlers) handlers.forEach((fn) => fn(payload.data, payload))
  }

  ws.onclose = () => {
    stopHeartbeat()
    ws = null
    notifyStatus(false)
    scheduleReconnect()
  }

  ws.onerror = () => {
    // 错误后浏览器会自动触发 onclose，重连逻辑统一在 onclose 处理
    ws && ws.close()
  }
}

export function disconnectWebSocket() {
  manualClose = true
  stopHeartbeat()
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws) {
    ws.close()
    ws = null
  }
}

/** 订阅事件（事件类型见 utils/constants.js 的 WS_EVENT） */
export function onEvent(eventType, handler) {
  if (!listeners.has(eventType)) listeners.set(eventType, new Set())
  listeners.get(eventType).add(handler)
}

export function offEvent(eventType, handler) {
  const handlers = listeners.get(eventType)
  if (handlers) handlers.delete(handler)
}

/** 断线重连成功回调（页面在此全量拉取一次数据，补偿断线期间的变更） */
export function onReconnected(handler) {
  reconnectHandlers.add(handler)
}

export function offReconnected(handler) {
  reconnectHandlers.delete(handler)
}

/** 连接状态回调（看板显示在线/离线角标） */
export function onStatusChange(handler) {
  statusHandlers.add(handler)
}

export function offStatusChange(handler) {
  statusHandlers.delete(handler)
}

// 内部：重连成功后通知订阅方（在 onopen 中区分首次/重连较繁琐，
// 简化为：只要 CONNECTED 事件到达即通知——服务端每次建连都会推 CONNECTED）
onEvent('CONNECTED', () => {
  reconnectHandlers.forEach((fn) => fn())
})
