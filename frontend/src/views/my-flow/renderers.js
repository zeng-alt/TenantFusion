import { NTag } from 'naive-ui'
import { h } from 'vue'

export const PROCESS_STATUS_MAP = {
  running: { type: 'processing', text: '进行中' },
  completed: { type: 'success', text: '已完成' },
  terminated: { type: 'error', text: '已终止' },
  suspended: { type: 'warning', text: '已挂起' },
}

export const ACTION_MAP = {
  approve: { type: 'success', text: '同意' },
  reject: { type: 'error', text: '驳回' },
  return: { type: 'warning', text: '退回' },
  transfer: { type: 'info', text: '转办' },
}

export function renderStatusTag(status) {
  const cfg = PROCESS_STATUS_MAP[status] || { type: 'default', text: status || '—' }
  return h(NTag, { size: 'small', type: cfg.type, bordered: false }, { default: () => cfg.text })
}

export function renderActionTag(action) {
  const cfg = ACTION_MAP[action] || { type: 'default', text: action || '—' }
  return h(NTag, { size: 'small', type: cfg.type, bordered: false }, { default: () => cfg.text })
}

export function renderDueTimeTag(dueTime) {
  return dueTime
    ? h(NTag, { size: 'small', type: 'warning', bordered: false }, { default: () => dueTime })
    : '—'
}
