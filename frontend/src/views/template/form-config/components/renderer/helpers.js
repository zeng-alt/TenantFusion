/**********************************
 * 表单渲染器通用工具函数
 **********************************/

import { FIELD_TYPE_META, hasOptions, isComposite } from '../../constants'

export { hasOptions, isComposite }

/** 安全解析 JSON 字符串，解析失败返回 fallback */
export function parseJson(str, fallback = null) {
  if (str == null || str === '')
    return fallback
  try {
    return JSON.parse(str)
  }
  catch {
    return fallback
  }
}

/** 解析字段附加属性 fieldProps（JSON 字符串 -> 对象） */
export function parseFieldProps(field) {
  return parseJson(field?.fieldProps, {}) || {}
}

/** 解析字段校验规则（JSON 字符串 -> 数组） */
export function parseValidationRules(field) {
  const rules = parseJson(field?.validationRules, [])
  return Array.isArray(rules) ? rules : []
}

/** 始终受保护、不允许被自定义属性覆盖的核心绑定键 */
const PROTECTED_BIND_PROPS = new Set([
  'value',
  'modelValue',
  'placeholder',
  'disabled',
  'size',
  'options',
  'onUpdate:value',
  'update:value',
  'on-update:value',
  'onUpdate:modelValue',
  'update:modelValue',
  'on-update:modelValue',
])

/** 文本值轻量类型推导：'true'/'false' -> 布尔，纯数字字符串 -> 数字，其余保持文本 */
function coerceBindValue(value) {
  if (value === 'true')
    return true
  if (value === 'false')
    return false
  if (value !== '' && !Number.isNaN(Number(value)))
    return Number(value)
  return value
}

/**
 * 构建可 v-bind 到 naive-ui 组件的自定义属性对象。
 * 读取 fieldProps.customAttrs（{key: value} 文本映射），
 * 剔除受保护的核心绑定键（value/placeholder/disabled/size/update 等），
 * 其余属性按存储类型推导后透传给组件。
 */
export function buildBindProps(field, extraReserved = []) {
  const props = parseFieldProps(field)
  const map = props.customAttrs && typeof props.customAttrs === 'object' ? props.customAttrs : {}
  const blocked = new Set([...PROTECTED_BIND_PROPS, ...extraReserved])
  const out = {}
  for (const [key, value] of Object.entries(map)) {
    if (key && !blocked.has(key))
      out[key] = coerceBindValue(value)
  }
  return out
}

/** 解析字段条件渲染表达式（JSON 字符串 -> 对象） */
export function parseVisibilityCondition(field) {
  return parseJson(field?.visibilityCondition, null)
}

/** 预览值对象中的字段键：优先 fieldKey，其次客户端临时 _key */
export function fieldValueKey(field) {
  return field?.fieldKey || field?._key || ''
}

/** 24 栅格列宽 -> 网格样式 */
export function gridSpanStyle(colSpan) {
  const span = Math.min(Math.max(Number(colSpan) || 24, 1), 24)
  return { gridColumn: `span ${span} / span ${span}` }
}

/** 字段类型图标配色，用于区分类型语义 */
export function typeColor(type) {
  const meta = FIELD_TYPE_META[type]
  if (!meta)
    return 'text-gray-400'
  if (meta.composite)
    return 'text-primary'
  if (['BOOLEAN', 'SELECT', 'MULTI_SELECT', 'DATE', 'DATETIME'].includes(type))
    return 'text-green-600 dark:text-green-500'
  if (['FILE', 'IMAGE', 'RICH_TEXT'].includes(type))
    return 'text-orange-500 dark:text-orange-400'
  return 'text-sky-500 dark:text-sky-400'
}
