// 时间格式化工具：统一显示格式
function pad(n) {
  return String(n).padStart(2, '0')
}

// 格式化为 yyyy-MM-dd HH:mm:ss（后端统一返回 ISO 时间字符串）
export function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// 格式化为 yyyy-MM-dd
export function formatDate(value) {
  return formatDateTime(value).slice(0, 10)
}
